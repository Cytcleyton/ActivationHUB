package com.salonhub.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Settings for talking to Meta, normally set in .env (META_APP_SECRET sets meta.app-secret, and so on).
@ConfigurationProperties(prefix = "meta")
public class MetaProperties {

    public enum Mode { LIVE, OFFLINE }

    // OFFLINE uses the built-in simulator and needs no Meta app.
    private Mode mode = Mode.OFFLINE;
    // App ID and Embedded Signup configuration ID are public; the browser needs them to open the pop-up.
    private String appId = "";
    private String configId = "";
    // The app secret stays on the server.
    private String appSecret = "";
    private String graphVersion = "v26.0";
    private String graphBaseUrl = "https://graph.facebook.com";
    private int timeoutSeconds = 15;
    // Meta Business Suite's page for business verification; the salon's portfolio ID is added to it.
    private String verificationUrl = "https://business.facebook.com/latest/settings/security_center";
    // Two-step verification PIN set on each salon's number when we register it.
    private String registrationPin = "246810";

    public boolean isLive() {
        return mode == Mode.LIVE;
    }

    // Everything the browser needs to open Meta's real pop-up.
    public boolean isLiveSignupReady() {
        return isLive() && !appId.isBlank() && !configId.isBlank();
    }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public String getGraphVersion() { return graphVersion; }
    public void setGraphVersion(String graphVersion) { this.graphVersion = graphVersion; }
    public String getGraphBaseUrl() { return graphBaseUrl; }
    public void setGraphBaseUrl(String graphBaseUrl) { this.graphBaseUrl = graphBaseUrl; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public String getVerificationUrl() { return verificationUrl; }
    public void setVerificationUrl(String verificationUrl) { this.verificationUrl = verificationUrl; }
    public String getRegistrationPin() { return registrationPin; }
    public void setRegistrationPin(String registrationPin) { this.registrationPin = registrationPin; }
}
