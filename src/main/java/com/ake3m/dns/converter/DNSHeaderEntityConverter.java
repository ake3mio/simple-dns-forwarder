package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSHeader;
import com.ake3m.dns.model.Rcode;

/**
 * This was implemented following the RFC 1035
 * <a href="https://www.rfc-editor.org/rfc/rfc1035#section-4.1.1">4.1.1. Header section format</a>
 */
public class DNSHeaderEntityConverter {

    public static final int UNSIGNED_MASK = 0xFF;
    public static final int NIBBLE_MASK = 0xF;
    public static final int THREE_BIT_MASK = 0x7;
    public static final int BIT_MASK = 0x1;

    public Result<DNSHeader> read(byte[] in) {
        int id = (in[0] & UNSIGNED_MASK) << 8 | in[1] & UNSIGNED_MASK;
        int flags = (in[2] & UNSIGNED_MASK) << 8 | in[3] & UNSIGNED_MASK;
        int qr = flags >> 15;
        int opcode = (flags >> 11) & NIBBLE_MASK;
        int aa = (flags >> 10) & BIT_MASK;
        int tc = (flags >> 9) & BIT_MASK;
        int rd = (flags >> 8) & BIT_MASK;
        int ra = (flags >> 7) & BIT_MASK;
        int z = (flags >> 4) & THREE_BIT_MASK;
        int rcode = flags & NIBBLE_MASK;
        return new Result<>(new DNSHeader(id, qr, opcode, aa, tc, rd, ra, z, Rcode.fromInt(rcode)), 2);
    }
}
