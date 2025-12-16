package com.ake3m.dns.handling.interceptor;

import com.ake3m.dns.handling.DNSHandler;
import com.ake3m.dns.model.DNSMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseInterceptor implements DNSHandler.Interceptor {
    private static final Logger log = LoggerFactory.getLogger(ResponseInterceptor.class);

    @Override
    public DNSMessage intercept(DNSMessage message, DNSHandler.Chain chain) {
        log.info("Received response: {}", message);
        return chain.next(message);
    }
}
