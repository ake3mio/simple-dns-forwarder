package com.ake3m.dns.handling;

import com.ake3m.dns.converter.DNSHeaderEntityConverter;
import com.ake3m.dns.converter.DNSQuestionEntityConverter;
import com.ake3m.dns.converter.DNSRecordEntityConverter;
import com.ake3m.dns.converter.Result;
import com.ake3m.dns.model.DNSHeader;
import com.ake3m.dns.model.DNSMessage;
import com.ake3m.dns.model.DNSQuestion;
import com.ake3m.dns.model.DNSRecord;
import com.ake3m.dns.model.Rcode;

public class Converter {
    private final DNSHeaderEntityConverter dnsHeaderEntityConverter;
    private final DNSQuestionEntityConverter dnsQuestionEntityConverter;
    private final DNSRecordEntityConverter dnsRecordEntityConverter;

    public Converter(
            DNSHeaderEntityConverter dnsHeaderEntityConverter,
            DNSQuestionEntityConverter dnsQuestionEntityConverter,
            DNSRecordEntityConverter dnsRecordEntityConverter
    ) {
        this.dnsHeaderEntityConverter = dnsHeaderEntityConverter;
        this.dnsQuestionEntityConverter = dnsQuestionEntityConverter;
        this.dnsRecordEntityConverter = dnsRecordEntityConverter;
    }

    public DNSMessage toDNSErrorResponse(DNSMessage message) {
        DNSHeader header = message.header();
        return new DNSMessage(
                new DNSHeader(
                        header.id(),
                        1,
                        header.opcode(),
                        0,
                        0,
                        0,
                        0,
                        0,
                        Rcode.SERVFAIL,
                        0,
                        0,
                        0,
                        0
                ),
                new DNSQuestion[]{},
                new DNSRecord[]{},
                new DNSRecord[]{},
                new DNSRecord[]{}
        );
    }

    public DNSMessage toDNSRequest(byte[] in) {
        Result<DNSHeader> headerResult = dnsHeaderEntityConverter.read(in);
        DNSHeader header = headerResult.value();
        DNSQuestion[] questions = new DNSQuestion[header.qdcount()];

        int offset = headerResult.offset();
        for (int i = 0; i < header.qdcount(); i++) {
            Result<DNSQuestion> questionResult = dnsQuestionEntityConverter.read(in, offset);
            DNSQuestion question = questionResult.value();
            questions[i] = question;
            offset = questionResult.offset();
        }

        return new DNSMessage(
                header,
                questions,
                new DNSRecord[]{},
                new DNSRecord[]{},
                new DNSRecord[]{}
        );
    }

    public byte[] toDNSRequest(DNSMessage message) {
        byte[] in = new byte[512];
        int offset = dnsHeaderEntityConverter.write(message.header(), in);
        for (DNSQuestion question : message.questions()) {
            offset = dnsQuestionEntityConverter.write(question, offset, in);
        }
        return in;
    }

    public DNSMessage toDNSResponse(byte[] in) {
        Result<DNSHeader> headerResult = dnsHeaderEntityConverter.read(in);
        DNSHeader header = headerResult.value();
        DNSQuestion[] questions = new DNSQuestion[header.qdcount()];
        DNSRecord[] answers = new DNSRecord[header.ancount()];
        DNSRecord[] authorityRecords = new DNSRecord[header.nscount()];
        DNSRecord[] additionalRecords = new DNSRecord[header.arcount()];

        int offset = headerResult.offset();
        for (int i = 0; i < header.qdcount(); i++) {
            Result<DNSQuestion> questionResult = dnsQuestionEntityConverter.read(in, offset);
            DNSQuestion question = questionResult.value();
            questions[i] = question;
            offset = questionResult.offset();
        }

        for (int i = 0; i < header.ancount(); i++) {
            Result<DNSRecord> answerResult = dnsRecordEntityConverter.read(in, offset);
            DNSRecord answer = answerResult.value();
            answers[i] = answer;
            offset = answerResult.offset();
        }

        for (int i = 0; i < header.nscount(); i++) {
            Result<DNSRecord> authorityRecordResult = dnsRecordEntityConverter.read(in, offset);
            DNSRecord authorityRecord = authorityRecordResult.value();
            authorityRecords[i] = authorityRecord;
            offset = authorityRecordResult.offset();
        }

        for (int i = 0; i < header.arcount(); i++) {
            Result<DNSRecord> additionalRecordResult = dnsRecordEntityConverter.read(in, offset);
            DNSRecord additionalRecord = additionalRecordResult.value();
            additionalRecords[i] = additionalRecord;
            offset = additionalRecordResult.offset();
        }

        return new DNSMessage(
                header,
                questions,
                answers,
                authorityRecords,
                additionalRecords
        );
    }

    public byte[] toDNSResponse(DNSMessage message) {
        final byte[] out = new byte[512];
        int offset = dnsHeaderEntityConverter.write(message.header(), out);

        for (DNSQuestion question : message.questions()) {
            offset = dnsQuestionEntityConverter.write(question, offset, out);
        }

        for (DNSRecord answer : message.answers()) {
            offset = dnsRecordEntityConverter.write(answer, offset, out);
        }

        for (DNSRecord answer : message.authorityRecords()) {
            offset = dnsRecordEntityConverter.write(answer, offset, out);
        }

        for (DNSRecord answer : message.additionalRecords()) {
            offset = dnsRecordEntityConverter.write(answer, offset, out);
        }

        return out;
    }
}
