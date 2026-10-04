package com.xin.musictag.domain;

public class ProcessingException extends RuntimeException {
    private final String code;
    private final String stage;
    public ProcessingException(String code, String stage, String message) {
        super(message);
        this.code = code;
        this.stage = stage;
    }
    public String code() { return code; }
    public String stage() { return stage; }
}
