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
                        .exceptionally(throwable -> {
                            log.error("Error handling dns packet", throwable);
                            return converter.toDNSErrorResponse(request);
                        })
                        .thenAccept(response -> {
                            log.info("Responding to dns packet");
                            byte[] out = converter.toDNSResponse(response);
                            DatagramPacket responsePacket = new DatagramPacket(out, out.length, requestPacket.getSocketAddress());
                            send(serverSocket, responsePacket);
                        });
            }
        }
    }

    private static void send(DatagramSocket serverSocket, DatagramPacket responsePacket) {
        try {
            serverSocket.send(responsePacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface Handler {
        CompletableFuture<DNSMessage> handle(DNSMessage message);
    }
}
