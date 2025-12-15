package com.ake3m.dns.model;

public enum QClass {
    IN              (1),
    CS              (2),
    CH              (3),
    HS              (4);

    private final int code;

    QClass(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static QClass fromInt(int code) {
        for (QClass qclass : values()) {
            if (qclass.code == code) {
                return qclass;
            }
        }
        throw new IllegalArgumentException("Unknown QClass code: " + code);
    }
}
