package com.salonhub.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    // Load the sample salons on startup, if there aren't any yet.
    private boolean seedDemoData = true;

    public boolean isSeedDemoData() { return seedDemoData; }
    public void setSeedDemoData(boolean seedDemoData) { this.seedDemoData = seedDemoData; }
}
