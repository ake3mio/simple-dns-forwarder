package com.ake3m.dns.handling;

import com.ake3m.dns.model.Rcode;

import java.time.Duration;

public sealed interface DNSError {
    record UpstreamTimeout(Duration timeout) implements DNSError {}
    record UpstreamFailure(Throwable cause) implements DNSError {}
    record RcodeFailure(Rcode rcode) implements DNSError {}
    record InternalError(Throwable cause) implements DNSError {}
}