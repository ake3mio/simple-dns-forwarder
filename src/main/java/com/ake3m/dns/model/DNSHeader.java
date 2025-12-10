package com.ake3m.dns.model;

/**
 * This was implemented following the RFC 1035
 * <a href="https://www.rfc-editor.org/rfc/rfc1035#section-4.1.1">4.1.1. Header section format</a>
 */
public record DNSHeader(
        int id,
        int qr,
        int opcode,
        int aa,
        int tc,
        int rd,
        int ra,
        int z,
        Rcode rcode
) {
}
