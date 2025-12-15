package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSHeader;
import com.ake3m.dns.model.Rcode;

import static com.ake3m.dns.converter.ByteConverter.readBits;
import static com.ake3m.dns.converter.ByteConverter.readU16;

/**
 * This was implemented following the RFC 1035
 * <a href="https://www.rfc-editor.org/rfc/rfc1035#section-4.1.1">4.1.1. Header section format</a>
 */
public class DNSHeaderEntityConverter {

    public Result<DNSHeader> read(byte[] in) {
        int id = readU16(in, 0);
        int flags = readU16(in, 2);
        int qr = readBits(flags, 15, 1);
        int opcode = readBits(flags, 11, 4);
        int aa = readBits(flags, 10, 1);
        int tc = readBits(flags, 9, 1);
        int rd = readBits(flags, 8, 1);
        int ra = readBits(flags, 7, 1);
        int z = readBits(flags, 4, 3);
        int rcode = readBits(flags, 0, 4);
        int qdcount = readU16(in, 4);
        int ancount = readU16(in, 6);
        int nscount = readU16(in, 8);
        int arcount = readU16(in, 10);
        return new Result<>(
                new DNSHeader(id,
                        qr,
                        opcode,
                        aa,
                        tc,
                        rd,
                        ra,
                        z,
                        Rcode.fromInt(rcode),
                        qdcount,
                        ancount,
                        nscount,
                        arcount
                ),
                12);
    }

    public int write(DNSHeader header, byte[] out) {
        out[0] = (byte) ((header.id() >>> 8) & 0xFF);
        out[1] = (byte) (header.id() & 0xFF);

        int flags = 0;
        flags |= header.qr() << 15;
        flags |= header.opcode() << 11;
        flags |= header.aa() << 10;
        flags |= header.tc() << 9;
        flags |= header.rd() << 8;
        flags |= header.ra() << 7;
        flags |= header.z() << 4;
        flags |= header.rcode().code();

        out[2] = (byte) ((flags >>> 8) & 0xFF);
        out[3] = (byte) (flags & 0xFF);
        return 12;
    }
}
