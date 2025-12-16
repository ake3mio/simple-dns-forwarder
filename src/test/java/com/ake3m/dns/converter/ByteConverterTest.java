package com.ake3m.dns.converter;

import org.junit.jupiter.api.Test;

import static com.ake3m.dns.converter.ByteConverter.readName;
import static com.ake3m.dns.converter.ByteConverter.writeName;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteConverterTest {

    @Test
    void readU16() {
        int value1 = 258;
        int value2 = 3000;
        byte[] in = new byte[4];
        in[0] = (byte) (value1 >>> 8);
        in[1] = (byte) value1;
        in[2] = (byte) (value2 >>> 8);
        in[3] = (byte) value2;

        assertEquals(value1, ByteConverter.readU16(in, 0));
        assertEquals(value2, ByteConverter.readU16(in, 2));
    }

    @Test
    void readU32() {
        long value = 4_294_967_295L;
        byte[] in = new byte[4];
        in[0] = (byte) (value >>> 24);
        in[1] = (byte) (value >>> 16);
        in[2] = (byte) (value >>> 8);
        in[3] = (byte) value;

        assertEquals(value, ByteConverter.readU32(in, 0));
    }

    @Test
    void readBits() {
        int value1 = 255;
        int value2 = 4000;
        byte[] in = new byte[4];
        in[0] = (byte) value1;
        in[1] = (byte) (value2 >>> 8);
        in[2] = (byte) value2;

        long value = ByteConverter.readU32(in, 0);

        assertEquals(value1, ByteConverter.readBits((int) value, 24, 8));
        assertEquals(value2, ByteConverter.readBits((int) value, 8, 16));
    }

    @Test
    void writeU16() {
        int value = 258;
        byte[] out = new byte[2];
        ByteConverter.writeU16(out, 0, value);
        assertEquals(value, ByteConverter.readU16(out, 0));
    }

    @Test
    void shouldWriteQname() {
        byte[] out = new byte[512];
        String qname = "VENERA.ISI.EDU";
        int offset = writeName(qname, 0, out);
        Result<String> roundTripped = readName(out, 0);

        assertEquals(16, offset);
        assertEquals(qname, roundTripped.value());
    }
    @Test
    void shouldReadQnameWithCompressionPointer() {
        byte[] bytes = new byte[]{
                6, 'V', 'E', 'N', 'E', 'R', 'A',
                3, 'I', 'S', 'I',
                3, 'E', 'D', 'U',
                0x00,

                4, 'M', 'A', 'I', 'L',
                (byte) 0xC0, 0x07,
        };

        Result<String> q1 = readName(bytes, 0);
        assertEquals(16, q1.offset());
        assertEquals("VENERA.ISI.EDU", q1.value());

        Result<String> q2 = readName(bytes, q1.offset());
        assertEquals(23, q2.offset());
        assertEquals("MAIL.ISI.EDU", q2.value());
    }
}