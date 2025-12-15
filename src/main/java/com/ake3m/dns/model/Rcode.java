package com.ake3m.dns.model;


public enum Rcode {
    NOERROR,
    FORMERR,
    SERVFAIL,
    NAMEERR,
    NOTIMP,
    REFUSED;

    public int code() {
        return ordinal();
    }

    public static Rcode fromInt(int code) {
        return values()[code];
    }
}
