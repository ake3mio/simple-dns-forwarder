package com.ake3m.dns.converter;

import java.net.UnknownHostException;

import static java.net.InetAddress.getByAddress;
import static java.net.InetAddress.getByName;
import static java.util.Arrays.copyOfRange;

public class ByteConverter {

    private ByteConverter() {
    }

    public static Result<String> readTXT(byte[] in, int offset, int rdlength) {
        int end = offset + rdlength;
        StringBuilder sb = new StringBuilder();

        int off = offset;
        while (off < end) {
            int len = in[off] & 0xFF;
            off++;

            if (off + len > end) {
                throw new IllegalArgumentException("TXT record exceeds RDLENGTH");
            }

            for (int i = 0; i < len; i++) {
                sb.append((char) in[off + i]);
            }
            off += len;

            if (off < end) {
                sb.append(' ');
            }
        }

        return new Result<>(sb.toString(), end);
    }

    public static int writeTXT(String rdata, int offset, byte[] out) {
        if (rdata == null) {
            throw new IllegalArgumentException("rdata is null");
        }

        String trimmed = rdata.trim();
        if (trimmed.isEmpty()) {
            out[offset++] = 0;
            return offset;
        }

        String[] parts = trimmed.split("\\s+");
        for (String part : parts) {
            int len = part.length();
            if (len > 255) {
                throw new IllegalArgumentException("TXT chunk too long (>255): " + len);
            }
            out[offset++] = (byte) len;
            for (int i = 0; i < len; i++) {
                out[offset++] = (byte) part.charAt(i);
            }
        }

        return offset;
    }

    public static Result<String> readIPv6(byte[] in, int offset) {
        byte[] ip = copyOfRange(in, offset, offset + 16);

        try {
            return new Result<>(getByAddress(ip).getHostAddress(), offset + 16);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid AAAA address bytes", e);
        }
    }

    public static int writeIPv6(String ipv6, int offset, byte[] out) {
        try {
            byte[] addr = getByName(ipv6).getAddress();

            if (addr.length != 16) {
                throw new IllegalArgumentException("Not an IPv6 address: " + ipv6);
            }

            System.arraycopy(addr, 0, out, offset, 16);
            return offset + 16;

        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IPv6 address: " + ipv6, e);
        }
    }

    public static Result<String> readIPv4(byte[] in, int offset) {
        StringBuilder ip = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            ip.append(in[offset + i] & 0xFF).append('.');
        }
        return new Result<>(ip.substring(0, ip.length() - 1), offset + 4);
    }

    public static int writeIPv4(String ipv4, int offset, byte[] out) {
        String[] parts = ipv4.split("\\.");
        out[offset++] = (byte) Integer.parseInt(parts[0]);
        out[offset++] = (byte) Integer.parseInt(parts[1]);
        out[offset++] = (byte) Integer.parseInt(parts[2]);
        out[offset++] = (byte) Integer.parseInt(parts[3]);
        return offset;
    }

    public static int writeHex(String hex, int offset, byte[] out) {
        String s = hex.trim();
        if ((s.length() & 1) != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        for (int i = 0; i < s.length(); i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Invalid hex at index " + i + ": " + s.substring(i, i + 2));
            }
            out[offset++] = (byte) ((hi << 4) | lo);
        }

        return offset;
    }

    public static Result<String> readSOA(byte[] in, int offset) {
        Result<String> mname = readName(in, offset);
        Result<String> rname = readName(in, mname.offset());

        int off = rname.offset();

        long serial = readU32(in, off);
        off += 4;
        long refresh = readU32(in, off);
        off += 4;
        long retry = readU32(in, off);
        off += 4;
        long expire = readU32(in, off);
        off += 4;
        long minimum = readU32(in, off);
        off += 4;

        String rdata = mname.value() + " " +
                rname.value() + " " +
                serial + " " +
                refresh + " " +
                retry + " " +
                expire + " " +
                minimum;

        return new Result<>(rdata, off);
    }

    public static int writeSOA(String rdata, int offset, byte[] out) {
        if (rdata == null) throw new IllegalArgumentException("rdata is null");

        String[] parts = rdata.trim().split("\\s+");
        if (parts.length != 7) {
            throw new IllegalArgumentException(
                    "SOA rdata must be: <mname> <rname> <serial> <refresh> <retry> <expire> <minimum>"
            );
        }

        String mname = parts[0];
        String rname = parts[1];

        long serial = Long.parseLong(parts[2]);
        long refresh = Long.parseLong(parts[3]);
        long retry = Long.parseLong(parts[4]);
        long expire = Long.parseLong(parts[5]);
        long minimum = Long.parseLong(parts[6]);

        offset = writeName(mname, offset, out);
        offset = writeName(rname, offset, out);

        offset = writeU32(out, offset, serial);
        offset = writeU32(out, offset, refresh);
        offset = writeU32(out, offset, retry);
        offset = writeU32(out, offset, expire);
        offset = writeU32(out, offset, minimum);

        return offset;
    }


    public static int readU16(byte[] in, int offset) {
        int result = 0;

        result |= (in[offset] & 0xFF) << 8;
        result |= (in[offset + 1] & 0xFF);

        return result;
    }

    public static int writeU16(byte[] out, int offset, int value) {
        out[offset++] = (byte) (value >>> 8);
        out[offset++] = (byte) value;
        return offset;
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

    public static int writeU32(byte[] out, int offset, long value) {
        out[offset++] = (byte) (value >>> 24);
        out[offset++] = (byte) (value >>> 16);
        out[offset++] = (byte) (value >>> 8);
        out[offset++] = (byte) value;
        return offset;
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

    public static Result<String> readName(byte[] in, int offset) {
        StringBuilder qname = new StringBuilder();
        boolean jumped = false;
        for (int i = offset; i < in.length; i++) {
            int length = in[i] & 0xFF;
            if (length == 0) {
                break;
            }

            if ((length & 0xC0) == 0xC0) {
                int pointer = in[i + 1] & 0xFF;
                i = pointer - 1;
                jumped = true;
                offset++;
                continue;
            }

            int adjustedIdx = 0;
            for (int j = 0; j < length; j++) {
                int idx = i + j + 1;
                qname.append((char) in[idx]);
                adjustedIdx = idx;
            }
            if (i + length + 1 < in.length) {
                qname.append('.');
            }

            i = adjustedIdx;
            if (!jumped) {
                offset = i + 1;
            }
        }

        if (!qname.isEmpty()) {
            qname.setLength(qname.length() - 1);
        }

        offset++;

        return new Result<>(qname.toString(), offset);
    }

    public static int writeName(String value, int offset, byte[] out) {
        String[] parts = value.split("\\.");

        for (String part : parts) {
            out[offset++] = (byte) part.length();
            for (int i = 0; i < part.length(); i++) {
                out[offset++] = (byte) part.charAt(i);
            }
        }
        out[offset++] = 0;
        return offset;
    }
}
