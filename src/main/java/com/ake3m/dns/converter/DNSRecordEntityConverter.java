package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSRecord;
import com.ake3m.dns.model.QClass;
import com.ake3m.dns.model.QType;

import static com.ake3m.dns.converter.ByteConverter.*;

public class DNSRecordEntityConverter {
    public Result<DNSRecord> read(byte[] in, int offset) {
        Result<String> qname = readName(in, offset);
        offset = qname.offset();
        QType qtype = QType.fromInt(ByteConverter.readU16(in, offset));
        offset += 2;
        int qclass = ByteConverter.readU16(in, offset);
        offset += 2;
        long ttl = ByteConverter.readU32(in, offset);
        offset += 4;
        int rdlength = ByteConverter.readU16(in, offset);
        offset += 2;
        Result<String> result = readRdata(in, offset, qtype, rdlength);
        String rdata = result.value();
        offset = result.offset();

        return new Result<>(new DNSRecord(qname.value(), qtype, QClass.fromInt(qclass), ttl, rdlength, rdata), offset);
    }

    public int write(DNSRecord dnsRecord, int offset, byte[] out) {
        offset = writeName(dnsRecord.name(), offset, out);
        offset = writeU16(out, offset, dnsRecord.qtype().code());
        offset = writeU16(out, offset, dnsRecord.qclass().code());
        offset = writeU32(out, offset, dnsRecord.ttl());
        offset = writeU16(out, offset, dnsRecord.rdlength());
        offset = writeRdata(out, offset, dnsRecord.rdata(), dnsRecord.qtype());
        return offset;
    }


    private static int writeRdata(byte[] out, int offset, String rdata, QType qtype) {
        return switch (qtype) {
            case NS,
                 CNAME,
                 PTR,
                 MINFO,
                 MX -> writeName(rdata, offset, out);
            case SOA -> writeSOA(rdata, offset, out);
            case TXT -> writeTXT(rdata, offset, out);
            case AAAA -> writeIPv6(rdata, offset, out);
            case A -> writeIPv4(rdata, offset, out);
            default -> writeHex(rdata, offset, out);
        };
    }

    private static Result<String> readRdata(byte[] in, int offset, QType qtype, int rdlength) {
        return switch (qtype) {
            case NS,
                 CNAME,
                 PTR,
                 MINFO,
                 MX -> readName(in, offset);
            case SOA -> readSOA(in, offset);
            case TXT -> readTXT(in, offset, rdlength);
            case AAAA -> readIPv6(in, offset);
            case A -> readIPv4(in, offset);
            default -> {
                StringBuilder hex = new StringBuilder();
                for (int i = 0; i < rdlength; i++) {
                    hex.append(String.format("%02X", in[offset + i]));
                }
                yield new Result<>(hex.toString(), offset + rdlength);
            }
        };
    }
}
