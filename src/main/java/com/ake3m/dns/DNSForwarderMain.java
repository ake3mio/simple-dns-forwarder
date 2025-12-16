package com.ake3m.dns;

import module java.base;
import com.ake3m.dns.converter.DNSHeaderEntityConverter;
import com.ake3m.dns.converter.DNSQuestionEntityConverter;
import com.ake3m.dns.converter.DNSRecordEntityConverter;
import com.ake3m.dns.handling.Converter;
import com.ake3m.dns.handling.DNSClient;
import com.ake3m.dns.handling.DNSHandler;
import com.ake3m.dns.handling.DNSServer;
import com.ake3m.dns.handling.interceptor.RequestInterceptor;
import com.ake3m.dns.handling.interceptor.ResponseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DNSForwarderMain {
    private static final Logger log = LoggerFactory.getLogger(DNSForwarderMain.class);

    public static void main(String[] args) throws IOException {

        Arguments arguments = Arguments.parse(args);

        log.info("Starting DNS forwarder with resolver {}:{}", arguments.resolverIp(), arguments.resolverPort());

        Converter converter = new Converter(
                new DNSHeaderEntityConverter(),
                new DNSQuestionEntityConverter(),
                new DNSRecordEntityConverter());

        DNSClient dnsClient = new DNSClient(arguments.resolverIp(), arguments.resolverPort(), Executors.newSingleThreadExecutor(), converter);
        List<DNSHandler.Interceptor> requestInterceptors = List.of(new RequestInterceptor());
        List<DNSHandler.Interceptor> responseInterceptors = List.of(new ResponseInterceptor());
        DNSHandler handler = new DNSHandler(dnsClient, requestInterceptors, responseInterceptors);
        DNSServer dnsServer = new DNSServer(arguments.serverPort(), handler, converter);

        dnsServer.start();
    }

    record Arguments(String resolverIp, int resolverPort, int serverPort) {
        public static Arguments parse(String[] args) {
            String resolverIp = "8.8.8.8";
            int resolverPort = 53;
            int serverPort = 2053;

            for (int i = 0; i < args.length; i++) {
                String part = args[i];
                if (part.equalsIgnoreCase("u")) {
                    var value = args[i + 1].split(":");
                    resolverIp = value[0].trim();
                    if (value.length > 1) {
                        resolverPort = Integer.parseInt(value[1].trim());
                    }
                }

                if (part.equalsIgnoreCase("p")) {
                    serverPort = Integer.parseInt(args[i + 1]);
                }
            }

            return new Arguments(resolverIp, resolverPort, serverPort);
        }
    }
}