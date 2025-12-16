package com.ake3m.dns.model;

/**
 * <a href="https://www.rfc-editor.org/rfc/rfc1035#section-3.2.2">QTYPE</a>
 */
public enum QType {

    A(1),
    NS(2),
    MD(3),
    MF(4),
    CNAME(5),
    SOA(6),
    MB(7),
    MG(8),
    MR(9),
    NULL(10),
    WKS(11),
    PTR(12),
    HINFO(13),
    MINFO(14),
    MX(15),
    TXT(16),
    // https://www.rfc-editor.org/rfc/rfc3596
    AAAA(28);
    private final int code;

    QType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static QType fromInt(int code) {
        for (QType qtype : values()) {
            if (qtype.code == code) {
                return qtype;
            }
        }
       throw new IllegalArgumentException("Unknown QType code: " + code);
    }
}
