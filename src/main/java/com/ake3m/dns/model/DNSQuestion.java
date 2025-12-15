package com.ake3m.dns.model;

public record DNSQuestion(
        String qname,
        QType qtype,
        QClass qclass
) {
}
