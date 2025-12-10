package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DNSHeaderEntityConverterTest {

    public static final int ID = 1234;
    private DNSHeaderEntityConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DNSHeaderEntityConverter();
    }

    @Nested
    class Reader {
        @Test
        void shouldReadId() {
            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;

            Result<DNSHeader> header = converter.read(in);

            assertEquals(ID, header.value().id());
        }

        @ValueSource(ints = {0, 1})
        @ParameterizedTest(name = "should read Query or Response={0}")
        void shouldReadQR(int value) {
            int flags = value << 15;

            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;

            Result<DNSHeader> header = converter.read(in);

            assertEquals(value, header.value().qr());
        }

        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
        @ParameterizedTest(name = "should read Opcode={0}")
        void shouldReadOpcode(int value) {
            int flags = value << 15;
            flags |= value << 11;

            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;

            Result<DNSHeader> header = converter.read(in);

            assertEquals(value, header.value().opcode());
        }

        @ValueSource(ints = {0, 1})
        @ParameterizedTest(name = "should read AA={0}")
        void shouldReadAA(int value) {
            int flags = value << 15;
            flags |= value << 11;
            flags |= value << 10;

            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;

            Result<DNSHeader> header = converter.read(in);

            assertEquals(value, header.value().aa());
        }

        @ValueSource(ints = {0, 1})
        @ParameterizedTest(name = "should read TC={0}")
        void shouldReadTC(int value) {
            int flags = value << 15;
            flags |= value << 11;
            flags |= value << 10;
            flags |= value << 9;

            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;

            Result<DNSHeader> header = converter.read(in);

            assertEquals(value, header.value().tc());
        }

        @ValueSource(ints = {0, 1})
        @ParameterizedTest(name = "should read RD={0}")
        void shouldReadRD(int value) {
            int flags = 1 << 15;
            flags |= value << 11;
            flags |= value << 10;
            flags |= value << 9;
            flags |= value << 8;

            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;

            Result<DNSHeader> header = converter.read(in);

            assertEquals(value, header.value().rd());
        }

        @ValueSource(ints = {0, 1})
        @ParameterizedTest(name = "should read RA={0}")
        void shouldReadRA(int value) {
            int flags = 1 << 15;
            flags |= value << 11;
            flags |= value << 10;
            flags |= value << 9;
            flags |= value << 8;
            flags |= value << 7;

            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;

            Result<DNSHeader> header = converter.read(in);

            assertEquals(value, header.value().ra());
        }

        @ValueSource(ints = {2, 1})
        @ParameterizedTest(name = "should read Z={0}")
        void shouldReadZ(int value) {
            int flags = 1 << 15;
            flags |= value << 11;
            flags |= value << 10;
            flags |= value << 9;
            flags |= value << 8;
            flags |= value << 7;
            flags |= value << 4;

            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;

            Result<DNSHeader> header = converter.read(in);

            assertEquals(value, header.value().z());
        }

        @ValueSource(ints = {0, 1, 2, 3, 4, 5})
        @ParameterizedTest(name = "should read RCODE={0}")
        void shouldReadRcode(int value) {
            int flags = createMockFlags(value);

            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;

            Result<DNSHeader> header = converter.read(in);

            assertEquals(value, header.value().rcode().code());
        }

        @Test
        void shouldReadQdcount() {
            int flags = createMockFlags(5);
            int qdcount = 1;
            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;
            in[4] = (byte) (qdcount >>> 8);
            in[5] = (byte) qdcount;

            Result<DNSHeader> header = converter.read(in);
            assertEquals(qdcount, header.value().qdcount());
        }

        @Test
        void shouldReadAncount() {
            int flags = createMockFlags(5);
            int qdcount = 1;
            int ancount = 5;
            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;
            in[4] = (byte) (qdcount >>> 8);
            in[5] = (byte) qdcount;
            in[6] = (byte) (ancount >>> 8);
            in[7] = (byte) ancount;

            Result<DNSHeader> header = converter.read(in);
            assertEquals(ancount, header.value().ancount());
        }

        @Test
        void shouldReadNscount() {
            int flags = createMockFlags(5);
            int qdcount = 1;
            int ancount = 5;
            int nscount = 10;

            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;
            in[4] = (byte) (qdcount >>> 8);
            in[5] = (byte) qdcount;
            in[6] = (byte) (ancount >>> 8);
            in[7] = (byte) ancount;
            in[8] = (byte) (nscount >>> 8);
            in[9] = (byte) nscount;

            Result<DNSHeader> header = converter.read(in);
            assertEquals(nscount, header.value().nscount());
        }

        @Test
        void shouldReadArcount() {
            int flags = createMockFlags(5);
            int qdcount = 1;
            int ancount = 5;
            int nscount = 10;
            int arcount = 100;

            byte[] in = new byte[12];
            in[0] = (byte) (ID >>> 8);
            in[1] = (byte) ID;
            in[2] = (byte) (flags >>> 8);
            in[3] = (byte) flags;
            in[4] = (byte) (qdcount >>> 8);
            in[5] = (byte) qdcount;
            in[6] = (byte) (ancount >>> 8);
            in[7] = (byte) ancount;
            in[8] = (byte) (nscount >>> 8);
            in[9] = (byte) nscount;
            in[10] = (byte) (arcount >>> 8);
            in[11] = (byte) arcount;

            Result<DNSHeader> header = converter.read(in);
            assertEquals(arcount, header.value().arcount());
        }

        private static int createMockFlags(int value) {
            int flags = 1 << 15;
            flags |= value << 11;
            flags |= value << 10;
            flags |= value << 9;
            flags |= value << 8;
            flags |= value << 7;
            flags |= value << 4;
            flags |= value;
            return flags;
        }
    }
}