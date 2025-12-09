package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSHeader;

public class DNSHeaderEntityConverter {

    public static final int UNSIGNED_MASK = 0xFF;

    public Result<DNSHeader> read(byte[] in) {
        int id = (in[0] & UNSIGNED_MASK) << 8 | in[1] & UNSIGNED_MASK;
        int flags = (in[2] & UNSIGNED_MASK) << 8 | in[3] & UNSIGNED_MASK;
        int qr = flags >> 15;
        return new Result<>(new DNSHeader(id, qr), 2);
    }
}
