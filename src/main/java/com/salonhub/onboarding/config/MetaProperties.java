package com.salonhub.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meta")
public class MetaProperties {

    // Two-step verification PIN set on each salon's number when we register it.
    private String registrationPin = "246810";

    public String getRegistrationPin() { return registrationPin; }
    public void setRegistrationPin(String registrationPin) { this.registrationPin = registrationPin; }
}
