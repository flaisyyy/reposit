package com.ua.mytrinity.tv;

public class AuthorizationException extends ProtocolException {
    private static final long serialVersionUID = -3295692416259305089L;

    public AuthorizationException() {
        super(1, "No authrorization");
    }

    public AuthorizationException(String detail) {
        super(1, detail);
    }
}
