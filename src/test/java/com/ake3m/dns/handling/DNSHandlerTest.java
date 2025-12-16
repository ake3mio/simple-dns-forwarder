package com.ake3m.dns.handling;

import com.ake3m.dns.converter.DNSHeaderEntityConverter;
import com.ake3m.dns.converter.DNSQuestionEntityConverter;
import com.ake3m.dns.converter.DNSRecordEntityConverter;
import com.ake3m.dns.model.DNSHeader;
import com.ake3m.dns.model.DNSMessage;
import com.ake3m.dns.model.DNSQuestion;
import com.ake3m.dns.model.DNSRecord;
import com.ake3m.dns.model.QClass;
import com.ake3m.dns.model.QType;
import com.ake3m.dns.model.Rcode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DNSHandlerTest {
    private final DNSHeaderEntityConverter headerConv = new DNSHeaderEntityConverter();
    private final DNSQuestionEntityConverter questionConv = new DNSQuestionEntityConverter();
    private final DNSRecordEntityConverter recordConv = new DNSRecordEntityConverter();
    private final Converter converter = new Converter(headerConv, questionConv, recordConv);

    private ExecutorService executor;

    @AfterEach
    void cleanup() {
        if (executor != null) executor.shutdownNow();
    }

    @Test
    void handlerAppliesInterceptorsAndUsesClient() throws Exception {
        try (DatagramSocket server = new DatagramSocket(0)) {
            int port = server.getLocalPort();
            server.setSoTimeout(3000);

            AtomicReference<String> observedQName = new AtomicReference<>();
            CountDownLatch serverDone = new CountDownLatch(1);

            Thread t = new Thread(() -> {
                try {
                    byte[] buf = new byte[512];
                    DatagramPacket req = new DatagramPacket(buf, buf.length);
                    server.receive(req);
                    byte[] in = new byte[req.getLength()];
                    System.arraycopy(req.getData(), 0, in, 0, req.getLength());
                    DNSMessage request = converter.toDNSRequest(in);
                    observedQName.set(request.questions()[0].qname());

                    DNSHeader h = request.header();
                    DNSHeader respHeader = new DNSHeader(h.id(), 1, h.opcode(), 0, 0, h.rd(), 0, 0, Rcode.NOERROR,
                            request.questions().length, 1, 0, 0);
                    String name = request.questions()[0].qname();
                    DNSQuestion q = new DNSQuestion(name, QType.A, QClass.IN);
                    DNSRecord a = new DNSRecord(name, QType.A, QClass.IN, 10, 4, "1.2.3.4");
                    DNSMessage resp = new DNSMessage(respHeader, new DNSQuestion[]{q}, new DNSRecord[]{a}, new DNSRecord[]{}, new DNSRecord[]{});
                    byte[] out = converter.toDNSResponse(resp);
                    DatagramPacket p = new DatagramPacket(out, out.length, req.getSocketAddress());
                    server.send(p);
                } catch (Exception ignored) {
                } finally {
                    serverDone.countDown();
                }
            });
            t.setDaemon(true);
            t.start();

            executor = Executors.newSingleThreadExecutor();
            DNSClient client = new DNSClient("127.0.0.1", port, executor, converter);

            DNSHandler.Interceptor reqInt = (msg, chain) -> {
                DNSQuestion q = msg.questions()[0];
                DNSQuestion modQ = new DNSQuestion("PREFIX." + q.qname(), q.qtype(), q.qclass());
                return new DNSMessage(msg.header(), new DNSQuestion[]{modQ}, msg.answers(), msg.authorityRecords(), msg.additionalRecords());
            };

            DNSHandler.Interceptor respInt = (msg, chain) -> {
                DNSRecord rec = msg.answers()[0];
                DNSRecord mod = new DNSRecord(rec.name(), rec.qtype(), rec.qclass(), rec.ttl(), rec.rdlength(), "9.9.9.9");
                return new DNSMessage(msg.header(), msg.questions(), new DNSRecord[]{mod}, msg.authorityRecords(), msg.additionalRecords());
            };

            DNSHandler handler = new DNSHandler(client, List.of(reqInt), List.of(respInt));

            DNSHeader h = new DNSHeader(0x7788, 0, 0, 0, 0, 1, 0, 0, Rcode.NOERROR, 1, 0, 0, 0);
            DNSQuestion q = new DNSQuestion("EXAMPLE.COM", QType.A, QClass.IN);
            DNSMessage request = new DNSMessage(h, new DNSQuestion[]{q}, new DNSRecord[]{}, new DNSRecord[]{}, new DNSRecord[]{});

            DNSMessage response = handler.handle(request).get(3, TimeUnit.SECONDS);

            assertEquals("PREFIX.EXAMPLE.COM", observedQName.get());
            assertEquals(1, response.answers().length);
            assertEquals("9.9.9.9", response.answers()[0].rdata());

            assertTrue(serverDone.await(1, TimeUnit.SECONDS));
            client.close();
        }
    }
}
