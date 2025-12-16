package com.ake3m.dns.handling;

import com.ake3m.dns.model.DNSMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class DNSHandler implements DNSServer.Handler {
    private final DNSClient dnsClient;
    private final Function<DNSMessage, DNSMessage> requestInterceptor;
    private final Function<DNSMessage, DNSMessage> responseInterceptor;

    public DNSHandler(DNSClient dnsClient, List<Interceptor> requestInterceptors, List<Interceptor> responseInterceptors) {
        this.dnsClient = dnsClient;
        this.requestInterceptor = intercept(requestInterceptors);
        this.responseInterceptor = intercept(responseInterceptors);
    }

    @Override
    public CompletableFuture<Either<DNSError, DNSMessage>> handle(DNSMessage request) {
        return dnsClient
                .forward(requestInterceptor.apply(request))
                .thenApply(e -> e.mapRight(responseInterceptor));
    }

    private Function<DNSMessage, DNSMessage> intercept(List<Interceptor> interceptors) {
        return (DNSMessage message) -> {
            Chain chain = msg -> msg;
            for (Interceptor interceptor : interceptors) {
                message = interceptor.intercept(message, chain);
            }
            return message;
        };
    }

    public interface Interceptor {
        DNSMessage intercept(DNSMessage request, Chain chain);
    }

    public interface Chain {
        DNSMessage next(DNSMessage request);
    }
}
