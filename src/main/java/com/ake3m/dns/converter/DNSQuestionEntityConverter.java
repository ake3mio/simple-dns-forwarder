package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSQuestion;
import com.ake3m.dns.model.QClass;
import com.ake3m.dns.model.QType;

public class DNSQuestionEntityConverter {
    public Result<DNSQuestion> read(byte[] in, int offset) {
        StringBuilder qname = new StringBuilder();

        for (int i = offset; i < in.length; i++) {
            int length = in[i] & 0xFF;
            if (length == 0) {
                break;
            }

            int adjustedIdx = 0;
            for (int j = 0; j < length; j++) {
                int idx = i + j + 1;
                qname.append((char) in[idx]);
                adjustedIdx = idx;
            }
            if (i + length + 1 < in.length) {
                qname.append('.');
            }

            i = adjustedIdx;
            offset = i + 1;
        }

        if (!qname.isEmpty()) {
            qname.setLength(qname.length() - 1);
        }
        offset++;
        int qtype = ByteConverter.readU16(in, offset);
        offset += 2;
        int qclass = ByteConverter.readU16(in, offset);
        offset += 2;

        return new Result<>(new DNSQuestion(qname.toString(), QType.fromInt(qtype), QClass.fromInt(qclass)), offset);
    }
}
