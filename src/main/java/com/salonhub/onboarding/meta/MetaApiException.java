package com.salonhub.onboarding.meta;

// A call to Meta failed. metaCode is Meta's error code, when it sent one.
public class MetaApiException extends RuntimeException {

    private final Integer metaCode;

    public MetaApiException(String message, Integer metaCode) {
        super(message);
        this.metaCode = metaCode;
    }

    public Integer getMetaCode() {
        return metaCode;
    }

    // e.g. "(#100) Invalid code"
    public String summary() {
        return (metaCode == null ? "" : "(#" + metaCode + ") ") + getMessage();
    }
}
