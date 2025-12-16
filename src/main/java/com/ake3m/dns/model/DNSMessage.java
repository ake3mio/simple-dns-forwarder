package com.ake3m.dns.model;


import java.util.Arrays;

public record DNSMessage(
        DNSHeader header,
        DNSQuestion[] questions,
        DNSRecord[] answers,
        DNSRecord[] authorityRecords,
        DNSRecord[] additionalRecords) {

    @Override
    public String toString() {
        return "DNSMessage{" +
                "header=" + header +
                ", questions=" + Arrays.toString(questions) +
                ", answers=" + Arrays.toString(answers) +
                ", authorityRecords=" + Arrays.toString(authorityRecords) +
                ", additionalRecords=" + Arrays.toString(additionalRecords) +
                '}';
    }
}
