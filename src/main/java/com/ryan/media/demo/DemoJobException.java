package com.ryan.media.demo;
public final class DemoJobException extends RuntimeException {
    private final String code;
    public DemoJobException(String code, String message) { super(message); this.code = code; }
    public String code() { return code; }
}
