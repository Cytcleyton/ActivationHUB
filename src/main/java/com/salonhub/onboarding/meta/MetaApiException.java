package com.salonhub.onboarding.meta;

// A call to Meta failed. metaCode and metaSubcode are Meta's error codes, when it sent them.
public class MetaApiException extends RuntimeException {

    private final Integer metaCode;
    private final Integer metaSubcode;

    public MetaApiException(String message, Integer metaCode) {
        this(message, metaCode, null);
    }

    public MetaApiException(String message, Integer metaCode, Integer metaSubcode) {
        super(message);
        this.metaCode = metaCode;
        this.metaSubcode = metaSubcode;
    }

    public Integer getMetaCode() {
        return metaCode;
    }

    public Integer getMetaSubcode() {
        return metaSubcode;
    }

    // e.g. "(#100/36008) Error validating verification code"
    public String summary() {
        if (metaCode == null) {
            return getMessage();
        }
        return "(#" + metaCode + (metaSubcode == null ? "" : "/" + metaSubcode) + ") " + getMessage();
    }
}
