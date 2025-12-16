package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSRecord;
import com.ake3m.dns.model.QClass;
import com.ake3m.dns.model.QType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.ake3m.dns.converter.ByteConverter.writeU16;
import static com.ake3m.dns.converter.ByteConverter.writeU32;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DNSRecordEntityConverterTest {
    private DNSRecordEntityConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DNSRecordEntityConverter();
    }

    @Nested
    class Reader {
        private byte[] aRecordBytes() {
            byte[] bytes = new byte[22];
            System.arraycopy(new byte[]{6, 'V', 'E', 'N', 'E', 'R', 'A', 0x00}, 0, bytes, 0, 8);
            writeU16(bytes, 8, QType.A.code());
            writeU16(bytes, 10, QClass.IN.code());
            writeU32(bytes, 12, 300);
            writeU16(bytes, 16, 4);
            bytes[18] = (byte) 93;
            bytes[19] = (byte) 184;
            bytes[20] = (byte) 216;
            bytes[21] = (byte) 34;
            return bytes;
        }

        @Test
        void shouldReadQname() {
            Result<DNSRecord> recordResult = converter.read(aRecordBytes(), 0);

            assertEquals(22, recordResult.offset());
            assertEquals("VENERA", recordResult.value().name());
        }

        @Test
        void shouldReadQtype() {
            Result<DNSRecord> recordResult = converter.read(aRecordBytes(), 0);

            assertEquals(22, recordResult.offset());
            assertEquals(QType.A, recordResult.value().qtype());
        }

        @Test
        void shouldReadQClass() {
            Result<DNSRecord> recordResult = converter.read(aRecordBytes(), 0);

            assertEquals(22, recordResult.offset());
            assertEquals(QClass.IN, recordResult.value().qclass());
        }

        @Test
        void shouldReadTTL() {
            Result<DNSRecord> recordResult = converter.read(aRecordBytes(), 0);

            assertEquals(300, recordResult.value().ttl());
        }

        @Test
        void shouldReadRDataLength() {
            Result<DNSRecord> recordResult = converter.read(aRecordBytes(), 0);

            assertEquals(4, recordResult.value().rdlength());
        }

        @Test
        void shouldReadRdata() {
            Result<DNSRecord> recordResult = converter.read(aRecordBytes(), 0);

            assertEquals("93.184.216.34", recordResult.value().rdata());
        }
    }

    @Nested
    class Writer {
        @Test
        void shouldWriteAndReadARecord() {
            DNSRecord record = new DNSRecord("VENERA", QType.A, QClass.IN, 300, 4, "93.184.216.34");

            byte[] out = new byte[512];
            int offset = converter.write(record, 0, out);
            Result<DNSRecord> roundTripped = converter.read(out, 0);

            assertEquals(22, offset);
            assertEquals(record.name(), roundTripped.value().name());
            assertEquals(record.qtype(), roundTripped.value().qtype());
            assertEquals(record.qclass(), roundTripped.value().qclass());
            assertEquals(record.ttl(), roundTripped.value().ttl());
            assertEquals(record.rdlength(), roundTripped.value().rdlength());
            assertEquals(record.rdata(), roundTripped.value().rdata());
        }
    }
}