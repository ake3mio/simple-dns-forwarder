package com.ake3m.dns.model;

/**
 * This was implemented following the RFC 1035
 * <a href="https://www.rfc-editor.org/rfc/rfc1035#section-4.1.3">4.1.3. Resource record format</a>
 */
public record DNSRecord(
        String name,
        QType qtype,
        QClass qclass,
        long ttl,
        int rdlength,
        String rdata
) {
}
