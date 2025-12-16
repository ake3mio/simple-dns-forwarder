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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DNSClientTest {
    private final DNSHeaderEntityConverter headerConv = new DNSHeaderEntityConverter();
    private final DNSQuestionEntityConverter questionConv = new DNSQuestionEntityConverter();
    private final DNSRecordEntityConverter recordConv = new DNSRecordEntityConverter();
    private final Converter converter = new Converter(headerConv, questionConv, recordConv);

    private ExecutorService executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void forwardSendsRequestAndParsesResponse() throws Exception {
        try (DatagramSocket server = new DatagramSocket(0)) {
            server.setSoTimeout(3000);
            int port = server.getLocalPort();

            CountDownLatch handled = new CountDownLatch(1);
            Thread serverThread = new Thread(() -> {
                try {
                    byte[] buf = new byte[512];
                    DatagramPacket req = new DatagramPacket(buf, buf.length);
                    server.receive(req);

                    byte[] in = new byte[req.getLength()];
                    System.arraycopy(req.getData(), 0, in, 0, req.getLength());
                    DNSMessage request = converter.toDNSRequest(in);

                    DNSHeader h = request.header();
                    DNSHeader respHeader = new DNSHeader(
                            h.id(),
                            1,
                            h.opcode(),
                            0, 0, h.rd(), 0, 0,
                            Rcode.NOERROR,
                            request.questions().length, 1, 0, 0
                    );

                    String name = request.questions()[0].qname();
                    DNSQuestion q = new DNSQuestion(name, QType.A, QClass.IN);
                    DNSRecord a = new DNSRecord(name, QType.A, QClass.IN, 60, 4, "93.184.216.34");
                    DNSMessage response = new DNSMessage(
                            respHeader,
                            new DNSQuestion[]{q},
                            new DNSRecord[]{a},
                            new DNSRecord[]{},
                            new DNSRecord[]{}
                    );

                    byte[] out = converter.toDNSResponse(response);
                    DatagramPacket resp = new DatagramPacket(out, out.length, req.getSocketAddress());
                    server.send(resp);
                } catch (Exception ignored) {
                } finally {
                    handled.countDown();
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            executor = Executors.newSingleThreadExecutor();
            DNSClient client = new DNSClient("127.0.0.1", port, converter);

            DNSHeader header = new DNSHeader(0x4455, 0, 0, 0, 0, 1, 0, 0, Rcode.NOERROR, 1, 0, 0, 0);
            DNSQuestion q = new DNSQuestion("EXAMPLE.COM", QType.A, QClass.IN);
            DNSMessage request = new DNSMessage(header, new DNSQuestion[]{q}, new DNSRecord[]{}, new DNSRecord[]{}, new DNSRecord[]{});

            Either.Right<DNSError, DNSMessage> response = (Either.Right<DNSError, DNSMessage>) client.forward(request).get(3, TimeUnit.SECONDS);
            DNSMessage data = response.data();
            assertEquals(1, data.header().qr());
            assertEquals(1, data.answers().length);
            assertEquals("EXAMPLE.COM", data.answers()[0].name());
            assertEquals("93.184.216.34", data.answers()[0].rdata());

            assertTrue(handled.await(1, TimeUnit.SECONDS));

            client.close();
        }
    }
}
