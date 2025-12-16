package com.ake3m.dns.model;

/**
 * This was implemented following the RFC 1035
 * <a href="https://www.rfc-editor.org/rfc/rfc1035#section-4.1.2">4.1.2. Question section format</a>
 */
public record DNSQuestion(
        String qname,
        QType qtype,
        QClass qclass
) {
}
