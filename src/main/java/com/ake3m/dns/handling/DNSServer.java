package com.ake3m.dns.handling;

import com.ake3m.dns.model.DNSMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CompletableFuture;

public class DNSServer {
    private static final Logger log = LoggerFactory.getLogger(DNSServer.class);
    private final int port;
    private final Handler handler;
    private final Converter converter;

    public DNSServer(int port,
                     Handler handler,
                     Converter converter) {
        this.port = port;
        this.handler = handler;
        this.converter = converter;
    }

    public void start() throws IOException {
        try (DatagramSocket serverSocket = new DatagramSocket(port)) {
            while (true) {
                final byte[] requestBuffer = new byte[512];
                DatagramPacket requestPacket = new DatagramPacket(requestBuffer, requestBuffer.length);
                serverSocket.receive(requestPacket);
                byte[] in = requestPacket.getData();

                log.info("Received dns packet");
                DNSMessage request = converter.toDNSRequest(in);
                handler.handle(request)
                        .thenAccept(response -> {
                            switch (response) {
                                case Either.Left<DNSError, DNSMessage> left -> {
                                    log.error("Error handling dns packet: {}", left.error());
                                    send(left.data(), requestPacket, serverSocket);
                                }
                                case Either.Right<DNSError, DNSMessage> right -> {
                                    log.info("Responding to dns packet");
                                    send(right.data(), requestPacket, serverSocket);
                                }
                            }
                        });
            }
        }
    }

    private void send(DNSMessage left, DatagramPacket requestPacket, DatagramSocket serverSocket) {
        byte[] out = converter.toDNSResponse(left);
        DatagramPacket responsePacket = new DatagramPacket(out, out.length, requestPacket.getSocketAddress());
        send(serverSocket, responsePacket);
    }

    private static void send(DatagramSocket serverSocket, DatagramPacket responsePacket) {
        try {
            serverSocket.send(responsePacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface Handler {
        CompletableFuture<Either<DNSError, DNSMessage>> handle(DNSMessage message);
    }
}
