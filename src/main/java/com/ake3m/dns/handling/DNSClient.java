package com.ake3m.dns.handling;

import com.ake3m.dns.model.DNSMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class DNSClient {
    private final DatagramSocket socket;
    private final ExecutorService executorService;
    private final Converter converter;

    public DNSClient(
            String ip,
            int port,
            ExecutorService executorService,
            Converter converter) throws SocketException {
        this.executorService = executorService;
        this.converter = converter;
        this.socket = new DatagramSocket();
        socket.connect(new InetSocketAddress(ip, port));
    }

    public CompletableFuture<DNSMessage> forward(DNSMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            byte[] in = converter.toDNSRequest(message);
            forward(in);
            byte[] out = new byte[512];
            receive(out);
            return converter.toDNSResponse(out);
        }, executorService).orTimeout(30, TimeUnit.SECONDS);
    }

    private void receive(byte[] out) {
        DatagramPacket responsePacket = new DatagramPacket(out, out.length);
        try {
            socket.receive(responsePacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void forward(byte[] in) {
        DatagramPacket requestPacket = new DatagramPacket(in, in.length);
        try {
            socket.send(requestPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        socket.close();
    }
}
