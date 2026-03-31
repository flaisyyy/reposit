package com.ua.mytrinity.tv;

public class ProtocolException extends Exception {
    public static final int E_ACCOUNT_NOT_FOUND = 2;
    public static final int E_NOAUTH = 1;
    public static final int E_NOT_ALLOWED = 4;
    public static final int E_WRONG_PASSWORD = 3;
    private static final long serialVersionUID = 6477044750271089903L;

    public ProtocolException(int id, String detailMessage) {
        super(detailMessage);
    }

    static ProtocolException create(int id, String detailMessage) {
        switch (id) {
            case 1:
                return new AuthorizationException(detailMessage);
            default:
                return new ProtocolException(id, detailMessage);
        }
    }
}
