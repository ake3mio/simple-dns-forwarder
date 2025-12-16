# Simple DNS Forwarder

A minimal DNS forwarder implemented in Java 25. It listens for DNS queries over UDP, applies optional request/response
interceptors, forwards queries to an upstream DNS server, and returns parsed responses.

Highlights

- Pure Java, no external DNS libraries
- RFC 1035–style byte parsing/serialization (header, question, and record sections)
- Clear separation of concerns via converter classes
- Unit tests covering converters and networking (client, handler, server)

Project structure

- src/main/java/com/ake3m/dns/converter: Low-level byte encoders/decoders (header, question, record)
- src/main/java/com/ake3m/dns/model: Immutable message types (records) used across the pipeline
- src/main/java/com/ake3m/dns/handling: High-level components (Converter, DNSClient, DNSHandler, DNSServer)
- src/test/java: Tests for converters and network-handling components

Getting started

1) Build and test
    - ./gradlew test

2) Run a simple forwarder
    - See DNSForwarderMain for an example entry point you can adapt. It wires DNSServer, DNSHandler, DNSClient, and
      Converter.

You can also run the DNSServer directly by building a jar and running it:

1) ./gradlew shadowJar
2) java -jar ./build/libs/simple-dns-forwarder.jar

The application will listen on port 2053 for DNS queries and forward them to 8.8.8.8:53 by default. You can change these
settings in DNSForwarderMain or by providing command-line arguments `-p <port>` to configure the server port and
`-u <upstream-dns-server>` to configure the upstream DNS server.

Design notes

- ByteConverter and friends implement the on-the-wire DNS format (RFC 1035 4.1.x) and have round-trip tests for A, AAAA,
  NS, TXT, SOA.
- Converter composes the entity converters to parse/serialize whole DNS messages for request/response flows.
- DNSClient uses a connected DatagramSocket to forward packets and parse upstream responses into DNSMessage instances.
- DNSHandler supports request/response interceptors so you can implement policies (e.g., rewrite qname, override
  answers).
- DNSServer continuously receives UDP packets, converts them to DNSMessage, delegates to the handler, and responds.

Testing

- Converter, DNSQuestion/Record/Header converters: Byte-level round trips and specific cases
- DNSClient/DNSHandler/DNSServer: Use real UDP sockets with timeouts to validate interactions end-to-end

Limitations / Future work

- No TCP or DoT/DoH support
- No EDNS(0) or message compression beyond what tests require
- Static 512-byte buffers; dynamic sizing or truncation handling could be added
- Minimal CLI/configuration; consider adding flags for upstream server timeout and logging
- Support for multiple upstream servers
- Support reading DNS routing policies from a file

License

- MIT (see LICENSE)
