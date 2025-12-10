package com.ake3m.dns.converter;

public class ByteConverter {

    private ByteConverter() {
    }

    public static int readU16(byte[] in, int offset) {
        int result = 0;

        result |= (in[offset] & 0xFF) << 8;
        result |= (in[offset + 1] & 0xFF);

        return result;
    }

    public static long readU32(byte[] in, int offset) {
        long result = 0;

        int i = 0;
        result |= (long) (in[offset + i++] & 0xFF) << 24;
        result |= (long) (in[offset + i++] & 0xFF) << 16;
        result |= (long) (in[offset + i++] & 0xFF) << 8;
        result |= (long) (in[offset + i] & 0xFF);

        return result;
    }

    public static int readBits(int value, int offset, int length) {
        if (offset + length > 32 || length < 0 || offset < 0) {
            throw new IllegalArgumentException("Invalid bit range");
        }
        if (length == 0) {
            return 0;
        }
        if (length == 32) {
            return value;
        }
        return (value >>> offset) & ((1 << length) - 1);
    }
}
