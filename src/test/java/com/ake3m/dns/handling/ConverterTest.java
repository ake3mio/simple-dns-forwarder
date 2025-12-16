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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.ake3m.dns.converter.ByteConverter.writeIPv6;
import static com.ake3m.dns.converter.ByteConverter.writeName;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConverterTest {
    private DNSHeaderEntityConverter headerConv;
    private DNSQuestionEntityConverter questionConv;
    private DNSRecordEntityConverter recordConv;
    private Converter converter;

    @BeforeEach
    void setUp() {
        headerConv = new DNSHeaderEntityConverter();
        questionConv = new DNSQuestionEntityConverter();
        recordConv = new DNSRecordEntityConverter();
        converter = new Converter(headerConv, questionConv, recordConv);
    }

    @Test
    void toDnsErrorResponseBuildsServfail() {
        DNSHeader inHeader = new DNSHeader(
                0xABCD,
                0,
                0,
                1,
                1,
                1,
                1,
                0,
                Rcode.NOERROR,
                1, 0, 0, 0
        );
        DNSMessage in = new DNSMessage(inHeader, new DNSQuestion[]{}, new DNSRecord[]{}, new DNSRecord[]{}, new DNSRecord[]{});

        DNSMessage out = converter.toDNSErrorResponse(in);

        DNSHeader h = out.header();
        assertEquals(0xABCD, h.id());
        assertEquals(1, h.qr());
        assertEquals(Rcode.SERVFAIL, h.rcode());
        assertEquals(0, h.aa());
        assertEquals(0, h.tc());
        assertEquals(0, h.rd());
        assertEquals(0, h.ra());
        assertEquals(0, h.qdcount());
        assertEquals(0, h.ancount());
        assertEquals(0, h.nscount());
        assertEquals(0, h.arcount());
        assertEquals(0, out.questions().length);
        assertEquals(0, out.answers().length);
        assertEquals(0, out.authorityRecords().length);
        assertEquals(0, out.additionalRecords().length);
    }

    @Test
    void toDnsRequestReadParsesQuestions() {

        byte[] buf = new byte[512];
        int off = 0;
        DNSHeader header = new DNSHeader(0x1234, 0, 0, 0, 0, 1, 0, 0, Rcode.NOERROR, 2, 0, 0, 0);
        off = headerConv.write(header, buf);

        DNSQuestion q1 = new DNSQuestion("EXAMPLE.COM", QType.A, QClass.IN);
        DNSQuestion q2 = new DNSQuestion("WWW.EXAMPLE.COM", QType.AAAA, QClass.IN);
        off = questionConv.write(q1, off, buf);
        off = questionConv.write(q2, off, buf);

        byte[] packet = new byte[off];
        System.arraycopy(buf, 0, packet, 0, off);

        DNSMessage msg = converter.toDNSRequest(packet);
        assertEquals(2, msg.header().qdcount());
        assertEquals(0x1234, msg.header().id());
        assertEquals("EXAMPLE.COM", msg.questions()[0].qname());
        assertEquals(QType.A, msg.questions()[0].qtype());
        assertEquals("WWW.EXAMPLE.COM", msg.questions()[1].qname());
        assertEquals(QType.AAAA, msg.questions()[1].qtype());
        assertEquals(0, msg.answers().length);
        assertEquals(0, msg.authorityRecords().length);
        assertEquals(0, msg.additionalRecords().length);
    }

    @Test
    void toDnsRequestWriteRoundTrip() {
        DNSHeader header = new DNSHeader(0xBEEF, 0, 0, 0, 0, 1, 0, 0, Rcode.NOERROR, 2, 0, 0, 0);
        DNSQuestion q1 = new DNSQuestion("EXAMPLE.COM", QType.A, QClass.IN);
        DNSQuestion q2 = new DNSQuestion("HOST.EXAMPLE", QType.AAAA, QClass.IN);
        DNSMessage in = new DNSMessage(header, new DNSQuestion[]{q1, q2}, new DNSRecord[]{}, new DNSRecord[]{}, new DNSRecord[]{});

        byte[] bytes = converter.toDNSRequest(in);
        DNSMessage parsed = converter.toDNSRequest(bytes);

        assertEquals(in.header().id(), parsed.header().id());
        assertEquals(in.questions().length, parsed.questions().length);
        assertEquals(in.questions()[0].qname(), parsed.questions()[0].qname());
        assertEquals(in.questions()[0].qtype(), parsed.questions()[0].qtype());
        assertEquals(in.questions()[1].qname(), parsed.questions()[1].qname());
        assertEquals(in.questions()[1].qtype(), parsed.questions()[1].qtype());
    }

    @Test
    void toDnsResponseReadParsesAllSections() {
        byte[] buf = new byte[1024];
        int off = 0;
        DNSHeader header = new DNSHeader(0x1111, 1, 0, 0, 0, 1, 0, 0, Rcode.NOERROR, 1, 1, 1, 1);
        off = headerConv.write(header, buf);


        DNSQuestion q = new DNSQuestion("EXAMPLE.COM", QType.A, QClass.IN);
        off = questionConv.write(q, off, buf);


        DNSRecord a = new DNSRecord("EXAMPLE.COM", QType.A, QClass.IN, 300, 4, "93.184.216.34");
        off = recordConv.write(a, off, buf);


        String nsName = "ns1.example.com";
        byte[] tmp = new byte[256];
        int nsRdLen = writeName(nsName, 0, tmp);
        DNSRecord ns = new DNSRecord("EXAMPLE.COM", QType.NS, QClass.IN, 60, nsRdLen, nsName);
        off = recordConv.write(ns, off, buf);


        DNSRecord aaaa = new DNSRecord("HOST.EXAMPLE", QType.AAAA, QClass.IN, 120, 16, "2001:db8::1");
        off = recordConv.write(aaaa, off, buf);

        byte[] packet = new byte[off];
        System.arraycopy(buf, 0, packet, 0, off);

        DNSMessage msg = converter.toDNSResponse(packet);
        assertEquals(1, msg.header().qdcount());
        assertEquals(1, msg.header().ancount());
        assertEquals(1, msg.header().nscount());
        assertEquals(1, msg.header().arcount());

        assertEquals("EXAMPLE.COM", msg.questions()[0].qname());
        assertEquals("EXAMPLE.COM", msg.answers()[0].name());
        assertEquals("93.184.216.34", msg.answers()[0].rdata());
        assertEquals(QType.NS, msg.authorityRecords()[0].qtype());
        assertEquals(nsName, msg.authorityRecords()[0].rdata());

        assertEquals(QType.AAAA, msg.additionalRecords()[0].qtype());

        byte[] orig = new byte[16];
        System.arraycopy(packet, packet.length - 16, orig, 0, 16);
        byte[] norm = new byte[16];
        writeIPv6(msg.additionalRecords()[0].rdata(), 0, norm);
        for (int i = 0; i < 16; i++) assertEquals(orig[i], norm[i]);
    }

    @Test
    void toDnsResponseWriteRoundTrip() {
        DNSHeader header = new DNSHeader(0x2222, 1, 0, 0, 0, 1, 0, 0, Rcode.NOERROR, 1, 1, 1, 1);
        DNSQuestion q = new DNSQuestion("EXAMPLE.COM", QType.A, QClass.IN);
        DNSRecord a = new DNSRecord("EXAMPLE.COM", QType.A, QClass.IN, 300, 4, "93.184.216.34");
        String nsName = "ns1.example.com";
        byte[] tmp = new byte[256];
        int nsRdLen = writeName(nsName, 0, tmp);
        DNSRecord ns = new DNSRecord("EXAMPLE.COM", QType.NS, QClass.IN, 60, nsRdLen, nsName);
        DNSRecord aaaa = new DNSRecord("HOST.EXAMPLE", QType.AAAA, QClass.IN, 120, 16, "2001:db8::1");

        DNSMessage in = new DNSMessage(
                header,
                new DNSQuestion[]{q},
                new DNSRecord[]{a},
                new DNSRecord[]{ns},
                new DNSRecord[]{aaaa}
        );

        byte[] bytes = converter.toDNSResponse(in);
        DNSMessage back = converter.toDNSResponse(bytes);

        assertEquals(in.header().id(), back.header().id());
        assertEquals(in.header().qdcount(), back.header().qdcount());
        assertEquals(in.header().ancount(), back.header().ancount());
        assertEquals(in.header().nscount(), back.header().nscount());
        assertEquals(in.header().arcount(), back.header().arcount());

        assertEquals(in.questions()[0].qname(), back.questions()[0].qname());
        assertEquals(in.answers()[0].rdata(), back.answers()[0].rdata());
        assertEquals(in.authorityRecords()[0].rdata(), back.authorityRecords()[0].rdata());


        byte[] norm = new byte[16];
        writeIPv6(back.additionalRecords()[0].rdata(), 0, norm);
        byte[] viaIn = new byte[16];
        writeIPv6(in.additionalRecords()[0].rdata(), 0, viaIn);
        for (int i = 0; i < 16; i++) assertEquals(viaIn[i], norm[i]);
    }
}
