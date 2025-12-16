package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSQuestion;
import com.ake3m.dns.model.QClass;
import com.ake3m.dns.model.QType;

import static com.ake3m.dns.converter.ByteConverter.*;

public class DNSQuestionEntityConverter {
    public Result<DNSQuestion> read(byte[] in, int offset) {
        Result<String> qname = readName(in, offset);
        offset = qname.offset();
        int qtype = ByteConverter.readU16(in, offset);
        offset += 2;
        int qclass = ByteConverter.readU16(in, offset);
        offset += 2;

        return new Result<>(new DNSQuestion(qname.value(), QType.fromInt(qtype), QClass.fromInt(qclass)), offset);
    }


    public int write(DNSQuestion dnsQuestion, int offset, byte[] out) {
        offset = writeName(dnsQuestion.qname(), offset, out);
        offset = writeU16(out, offset, dnsQuestion.qtype().code());
        offset = writeU16(out, offset, dnsQuestion.qclass().code());

        return offset;
    }


}
