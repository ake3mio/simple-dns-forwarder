package com.ake3m.dns.converter;

import com.ake3m.dns.model.DNSQuestion;
import com.ake3m.dns.model.QClass;
import com.ake3m.dns.model.QType;

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

    Result<String> readName(byte[] in, int offset) {
        StringBuilder qname = new StringBuilder();
        boolean jumped = false;
        for (int i = offset; i < in.length; i++) {
            int length = in[i] & 0xFF;
            if (length == 0) {
                break;
            }

            if (length == 0xC0) {
                int pointer = in[i + 1] & 0xFF;
                i = pointer - 1;
                jumped = true;
                offset++;
                continue;
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
            if (!jumped) {
                offset = i + 1;
            }
        }

        if (!qname.isEmpty()) {
            qname.setLength(qname.length() - 1);
        }

        offset++;

        return new Result<>(qname.toString(), offset);
    }
}
