package com.ake3m.dns.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}