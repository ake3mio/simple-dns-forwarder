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
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DNSServerTest {
    @Test
    void serverReceivesPacketAndResponds() throws Exception {
        DNSHeaderEntityConverter h = new DNSHeaderEntityConverter();
        DNSQuestionEntityConverter q = new DNSQuestionEntityConverter();
        DNSRecordEntityConverter r = new DNSRecordEntityConverter();
        Converter converter = new Converter(h, q, r);

        int port;
        try (DatagramSocket probe = new DatagramSocket(0)) {
            port = probe.getLocalPort();
        }

        CountDownLatch started = new CountDownLatch(1);

        DNSServer.Handler handler = (req) -> {
            DNSHeader inH = req.header();
            DNSHeader outH = new DNSHeader(
                    inH.id(),
                    1,
                    inH.opcode(),
                    0, 0, inH.rd(), 0, 0,
                    Rcode.NOERROR,
                    req.questions().length,
                    1,
                    0,
                    0
            );
            String name = req.questions()[0].qname();
            DNSQuestion q1 = new DNSQuestion(name, QType.A, QClass.IN);
            DNSRecord a = new DNSRecord(name, QType.A, QClass.IN, 120, 4, "203.0.113.5");
            DNSMessage resp = new DNSMessage(outH, new DNSQuestion[]{q1}, new DNSRecord[]{a}, new DNSRecord[]{}, new DNSRecord[]{});
            return CompletableFuture.completedFuture(Either.right(resp));
        };

        DNSServer server = new DNSServer(port, handler, converter);

        Thread t = new Thread(() -> {
            started.countDown();
            try {
                server.start();
            } catch (Exception ignored) {
            }
        });
        t.setDaemon(true);
        t.start();

        assertTrue(started.await(1, TimeUnit.SECONDS));

        try (DatagramSocket client = new DatagramSocket()) {
            DNSHeader hdr = new DNSHeader(0x99AA, 0, 0, 0, 0, 1, 0, 0, Rcode.NOERROR, 1, 0, 0, 0);
            DNSQuestion qu = new DNSQuestion("EXAMPLE.COM", QType.A, QClass.IN);
            DNSMessage query = new DNSMessage(hdr, new DNSQuestion[]{qu}, new DNSRecord[]{}, new DNSRecord[]{}, new DNSRecord[]{});
            byte[] payload = converter.toDNSRequest(query);

            DatagramPacket packet = new DatagramPacket(payload, payload.length, new InetSocketAddress("127.0.0.1", port));
            client.send(packet);

            byte[] buf = new byte[512];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            client.setSoTimeout(30000);
            client.receive(resp);

            byte[] data = new byte[resp.getLength()];
            System.arraycopy(resp.getData(), 0, data, 0, resp.getLength());
            DNSMessage response = converter.toDNSResponse(data);

            assertEquals(1, response.header().qr());
            assertEquals(1, response.answers().length);
            assertEquals("EXAMPLE.COM", response.answers()[0].name());
            assertEquals("203.0.113.5", response.answers()[0].rdata());
        }
    }
}
