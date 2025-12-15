package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSQuestion;
import com.ake3m.dns.model.QClass;
import com.ake3m.dns.model.QType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.ake3m.dns.converter.ByteConverter.writeU16;
import static org.junit.jupiter.api.Assertions.*;

class DNSQuestionEntityConverterTest {
    private DNSQuestionEntityConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DNSQuestionEntityConverter();
    }

    @Nested
    class Reader {
        @Test
        void shouldReadQname() {
            byte[] bytes = new byte[]{6, 'V', 'E', 'N', 'E', 'R', 'A', 3, 'I', 'S', 'I', 3, 'E', 'D', 'U', 0x00, 0, 1, 0, 1};

            Result<DNSQuestion> questionResult = converter.read(bytes, 0);

            assertEquals(20, questionResult.offset());
            assertEquals("VENERA.ISI.EDU", questionResult.value().qname());
        }

        @Test
        void shouldReadQnameWithCompressionPointer() {
            byte[] bytes = new byte[]{
                    6, 'V', 'E', 'N', 'E', 'R', 'A',
                    3, 'I', 'S', 'I',
                    3, 'E', 'D', 'U',
                    0x00,
                    0, 1, 0, 1,

                    4, 'M', 'A', 'I', 'L',
                    (byte) 0xC0, 0x07,
                    0, 1, 0, 1
            };

            Result<DNSQuestion> q1 = converter.read(bytes, 0);
            assertEquals(20, q1.offset());
            assertEquals("VENERA.ISI.EDU", q1.value().qname());

            Result<DNSQuestion> q2 = converter.read(bytes, q1.offset());
            assertEquals(31, q2.offset());
            assertEquals("MAIL.ISI.EDU", q2.value().qname());
        }

        @EnumSource(QType.class)
        @ParameterizedTest
        void shouldReadQtype(QType qtype) {
            byte[] bytes = new byte[12];
            System.arraycopy(new byte[]{6, 'V', 'E', 'N', 'E', 'R', 'A', 0x00}, 0, bytes, 0, 8);
            writeU16(bytes, 8, qtype.code());
            writeU16(bytes, 10, 1);
            Result<DNSQuestion> questionResult = converter.read(bytes, 0);

            assertEquals(12, questionResult.offset());
            assertEquals(qtype, questionResult.value().qtype());
        }

        @EnumSource(QClass.class)
        @ParameterizedTest
        void shouldReadQClass(QClass qclass) {
            byte[] bytes = new byte[12];
            System.arraycopy(new byte[]{6, 'V', 'E', 'N', 'E', 'R', 'A', 0x00}, 0, bytes, 0, 8);
            writeU16(bytes, 8, 1);
            writeU16(bytes, 10, qclass.code());

            Result<DNSQuestion> questionResult = converter.read(bytes, 0);

            assertEquals(12, questionResult.offset());
            assertEquals(qclass, questionResult.value().qclass());
        }
    }
}