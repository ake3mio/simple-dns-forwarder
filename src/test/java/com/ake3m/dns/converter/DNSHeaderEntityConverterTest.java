package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DNSHeaderEntityConverterTest {

    public static final int ID = 1234;
    private DNSHeaderEntityConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DNSHeaderEntityConverter();
    }

    @Nested
    class Reader {
        @Test
        void readId() {
            byte[] in = {
                    (byte) (ID >>> 8),
                    (byte) ID
            };

            Result<DNSHeader> header = converter.read(in);

            assertEquals(ID, header.value().id());

        }

        @ValueSource(ints = {0, 1})
        @ParameterizedTest(name = "should read Query or Response={0}")
        void shouldReadQR(int value) {
            int flags = value << 15;

            byte[] in = {
                    (byte) (ID >>> 8),
                    (byte) ID,
                    (byte) (flags >>> 8),
                    (byte) flags,
            };

            Result<DNSHeader> header = converter.read(in);

            assertEquals(ID, header.value().id());
            assertEquals(value, header.value().qr());
        }
    }
}