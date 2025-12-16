package com.ake3m.dns.handling;

import com.ake3m.dns.model.DNSMessage;
import com.ake3m.dns.model.Rcode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.concurrent.*;

import static com.ake3m.dns.handling.Either.left;
import static com.ake3m.dns.handling.Either.right;

public final class DNSClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DNSClient.class);
    public static final int TIMEOUT_SECONDS = 30;

    private final DatagramSocket socket;
    private final ExecutorService executor;
    private final Converter converter;

    public DNSClient(
            String ip,
            int port,
            Converter converter
    ) throws SocketException {

        this.converter = converter;

        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "dns-client");
            t.setDaemon(true);
            return t;
        });

        this.socket = new DatagramSocket();
        this.socket.connect(new InetSocketAddress(ip, port));
        this.socket.setSoTimeout(TIMEOUT_SECONDS * 1000);
    }

    public CompletableFuture<Either<DNSError, DNSMessage>> forward(DNSMessage message) {
        return CompletableFuture
                .supplyAsync(() -> sendAndReceive(message), executor)
                .exceptionally(ex -> handleFailure(ex, message));
    }

    private Either<DNSError, DNSMessage> sendAndReceive(DNSMessage message) {
        try {

            synchronized (socket) {
                byte[] request = converter.toDNSRequest(message);
                socket.send(new DatagramPacket(request, request.length));

                byte[] buffer = new byte[512];
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket.receive(response);

                DNSMessage dnsResponse = converter.toDNSResponse(response.getData());

                if (dnsResponse.header().rcode() != Rcode.NOERROR) {
                    return left(
                            new DNSError.RcodeFailure(dnsResponse.header().rcode()),
                            converter.toDNSErrorResponse(dnsResponse)
                    );
                }

                return right(dnsResponse);
            }

        } catch (SocketTimeoutException e) {
            return left(
                    new DNSError.UpstreamTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)),
                    converter.toDNSErrorResponse(message)
            );
        } catch (IOException e) {
            return left(
                    new DNSError.UpstreamFailure(e),
                    converter.toDNSErrorResponse(message)
            );
        }
    }

    private Either<DNSError, DNSMessage> handleFailure(Throwable ex, DNSMessage message) {
        Throwable cause = unwrap(ex);
        log.error("DNS client failure", cause);

        return left(
                new DNSError.InternalError(cause),
                converter.toDNSErrorResponse(message)
        );
    }

    @Override
    public void close() {
        socket.close();
        executor.shutdownNow();
    }

    private static Throwable unwrap(Throwable t) {
        if (t instanceof CompletionException || t instanceof ExecutionException) {
            return t.getCause() != null ? t.getCause() : t;
        }
        return t;
    }
}
