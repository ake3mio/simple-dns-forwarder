package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSRecord;
import com.ake3m.dns.model.QClass;
import com.ake3m.dns.model.QType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.ake3m.dns.converter.ByteConverter.readName;
import static com.ake3m.dns.converter.ByteConverter.writeIPv4;
import static com.ake3m.dns.converter.ByteConverter.writeIPv6;
import static com.ake3m.dns.converter.ByteConverter.writeName;
import static com.ake3m.dns.converter.ByteConverter.writeSOA;
import static com.ake3m.dns.converter.ByteConverter.writeTXT;
import static com.ake3m.dns.converter.ByteConverter.writeU16;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DNSRecordEntityConverterTest {
    private DNSRecordEntityConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DNSRecordEntityConverter();
    }

    @Nested
    class Reader {
        @Test
        void readARecord() {
            byte[] buf = new byte[512];
            int off = 0;
            off = writeName("EXAMPLE.COM", off, buf);
            off = writeU16(buf, off, QType.A.code());
            off = writeU16(buf, off, QClass.IN.code());
            off = ByteConverter.writeU32(buf, off, 300);
            off = writeU16(buf, off, 4);
            off = writeIPv4("93.184.216.34", off, buf);

            byte[] bytes = new byte[off];
            System.arraycopy(buf, 0, bytes, 0, off);

            Result<DNSRecord> rr = converter.read(bytes, 0);
            assertEquals(bytes.length, rr.offset());
            assertEquals("EXAMPLE.COM", rr.value().name());
            assertEquals(QType.A, rr.value().qtype());
            assertEquals(QClass.IN, rr.value().qclass());
            assertEquals(300, rr.value().ttl());
            assertEquals(4, rr.value().rdlength());
            assertEquals("93.184.216.34", rr.value().rdata());
        }

        @Test
        void readAAAARecord() {
            byte[] buf = new byte[512];
            int off = 0;
            off = writeName("HOST.EXAMPLE", off, buf);
            off = writeU16(buf, off, QType.AAAA.code());
            off = writeU16(buf, off, QClass.IN.code());
            off = ByteConverter.writeU32(buf, off, 120);
            off = writeU16(buf, off, 16);
            off = writeIPv6("2001:db8::1", off, buf);

            byte[] bytes = new byte[off];
            System.arraycopy(buf, 0, bytes, 0, off);

            Result<DNSRecord> rr = converter.read(bytes, 0);
            assertEquals(bytes.length, rr.offset());
            assertEquals(QType.AAAA, rr.value().qtype());
            assertEquals(16, rr.value().rdlength());
            byte[] out = new byte[16];
            writeIPv6(rr.value().rdata(), 0, out);
            byte[] original = new byte[16];
            System.arraycopy(bytes, bytes.length - 16, original, 0, 16);
            for (int i = 0; i < 16; i++) {
                assertEquals(original[i], out[i]);
            }
        }

        @Test
        void readNSRecord() {
            byte[] buf = new byte[512];
            int off = 0;
            off = writeName("EXAMPLE.COM", off, buf);
            off = writeU16(buf, off, QType.NS.code());
            off = writeU16(buf, off, QClass.IN.code());
            off = ByteConverter.writeU32(buf, off, 60);
            int rdLenPos = off;
            off = writeU16(buf, off, 0);
            int rdStart = off;
            off = writeName("ns1.example.com", off, buf);
            int rdlength = off - rdStart;
            writeU16(buf, rdLenPos, rdlength);

            byte[] bytes = new byte[off];
            System.arraycopy(buf, 0, bytes, 0, off);

            Result<DNSRecord> rr = converter.read(bytes, 0);
            assertEquals(bytes.length, rr.offset());
            assertEquals(QType.NS, rr.value().qtype());
            assertEquals("ns1.example.com", rr.value().rdata());
            assertEquals(rdlength, rr.value().rdlength());
        }

        @Test
        void readTXTRecord() {
            byte[] buf = new byte[512];
            int off = 0;
            off = writeName("TXT.EXAMPLE", off, buf);
            off = writeU16(buf, off, QType.TXT.code());
            off = writeU16(buf, off, QClass.IN.code());
            off = ByteConverter.writeU32(buf, off, 10);
            int rdStart = off + 2;
            int rdoff = writeTXT("hello world", rdStart, buf);
            int rdlength = rdoff - rdStart;
            writeU16(buf, off, rdlength);
            off = rdoff;

            byte[] bytes = new byte[off];
            System.arraycopy(buf, 0, bytes, 0, off);

            Result<DNSRecord> rr = converter.read(bytes, 0);
            assertEquals(bytes.length, rr.offset());
            assertEquals(QType.TXT, rr.value().qtype());
            assertEquals("hello world", rr.value().rdata());
            assertEquals(rdlength, rr.value().rdlength());
        }

        @Test
        void readSOARecord() {
            byte[] buf = new byte[1024];
            int off = 0;
            off = writeName("EXAMPLE.COM", off, buf);
            off = writeU16(buf, off, QType.SOA.code());
            off = writeU16(buf, off, QClass.IN.code());
            off = ByteConverter.writeU32(buf, off, 100);
            int rdStart = off + 2;
            String soa = "ns.example.com hostmaster.example.com 2025010101 7200 3600 1209600 300";
            int rdoff = writeSOA(soa, rdStart, buf);
            int rdlength = rdoff - rdStart;
            writeU16(buf, off, rdlength);
            off = rdoff;

            byte[] bytes = new byte[off];
            System.arraycopy(buf, 0, bytes, 0, off);

            Result<DNSRecord> rr = converter.read(bytes, 0);
            assertEquals(bytes.length, rr.offset());
            assertEquals(QType.SOA, rr.value().qtype());
            assertEquals(soa, rr.value().rdata());
            assertEquals(rdlength, rr.value().rdlength());
        }
    }

    @Nested
    class Writer {
        @Test
        void writeARoundTrip() {
            String name = "EXAMPLE.COM";
            String ip = "93.184.216.34";
            int rdlength = 4;
            DNSRecord rr = new DNSRecord(name, QType.A, QClass.IN, 300, rdlength, ip);

            byte[] out = new byte[512];
            int end = converter.write(rr, 0, out);

            Result<String> n = readName(out, 0);
            assertEquals(name, n.value());

            Result<DNSRecord> back = converter.read(out, 0);
            assertEquals(end, back.offset());
            assertEquals(rr.name(), back.value().name());
            assertEquals(rr.qtype(), back.value().qtype());
            assertEquals(rr.qclass(), back.value().qclass());
            assertEquals(rr.ttl(), back.value().ttl());
            assertEquals(rr.rdlength(), back.value().rdlength());
            assertEquals(rr.rdata(), back.value().rdata());
        }

        @Test
        void writeAAAARoundTrip() {
            String name = "HOST.EXAMPLE";
            String ip = "2001:db8::1";
            int rdlength = 16;
            DNSRecord rr = new DNSRecord(name, QType.AAAA, QClass.IN, 120, rdlength, ip);

            byte[] out = new byte[512];
            int end = converter.write(rr, 0, out);

            Result<DNSRecord> back = converter.read(out, 0);
            assertEquals(end, back.offset());
            assertEquals(rr.qtype(), back.value().qtype());
            byte[] orig = new byte[16];
            System.arraycopy(out, end - 16, orig, 0, 16);
            byte[] norm = new byte[16];
            writeIPv6(back.value().rdata(), 0, norm);
            for (int i = 0; i < 16; i++) assertEquals(orig[i], norm[i]);
        }

        @Test
        void writeNSRoundTrip() {
            String name = "EXAMPLE.COM";
            String rdataName = "ns1.example.com";
            byte[] tmp = new byte[256];
            int rdlength = writeName(rdataName, 0, tmp);
            DNSRecord rr = new DNSRecord(name, QType.NS, QClass.IN, 60, rdlength, rdataName);

            byte[] out = new byte[512];
            int end = converter.write(rr, 0, out);
            Result<DNSRecord> back = converter.read(out, 0);
            assertEquals(end, back.offset());
            assertEquals(rdataName, back.value().rdata());
            assertEquals(rdlength, back.value().rdlength());
        }

        @Test
        void writeTXTRoundTrip() {
            String name = "TXT.EXAMPLE";
            String txt = "hello world";
            byte[] tmp = new byte[256];
            int rdlength = writeTXT(txt, 0, tmp);
            DNSRecord rr = new DNSRecord(name, QType.TXT, QClass.IN, 10, rdlength, txt);

            byte[] out = new byte[512];
            int end = converter.write(rr, 0, out);
            Result<DNSRecord> back = converter.read(out, 0);
            assertEquals(end, back.offset());
            assertEquals(txt, back.value().rdata());
            assertEquals(rdlength, back.value().rdlength());
        }

        @Test
        void writeSOARoundTrip() {
            String name = "EXAMPLE.COM";
            String soa = "ns.example.com hostmaster.example.com 2025010101 7200 3600 1209600 300";
            byte[] tmp = new byte[512];
            int rdlength = writeSOA(soa, 0, tmp);
            DNSRecord rr = new DNSRecord(name, QType.SOA, QClass.IN, 100, rdlength, soa);

            byte[] out = new byte[1024];
            int end = converter.write(rr, 0, out);
            Result<DNSRecord> back = converter.read(out, 0);
            assertEquals(end, back.offset());
            assertEquals(soa, back.value().rdata());
            assertEquals(rdlength, back.value().rdlength());
        }
    }
}