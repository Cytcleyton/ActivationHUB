package com.salonhub.onboarding.config;

import com.salonhub.onboarding.domain.OnboardingStage;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

// stall.thresholds[STAGE]=1d in application.properties; stages not listed keep StallPolicy's defaults.
@ConfigurationProperties(prefix = "stall")
public class StallProperties {

    private Map<OnboardingStage, Duration> thresholds = new EnumMap<>(OnboardingStage.class);

    public Map<OnboardingStage, Duration> getThresholds() { return thresholds; }
    public void setThresholds(Map<OnboardingStage, Duration> thresholds) { this.thresholds = thresholds; }
}
