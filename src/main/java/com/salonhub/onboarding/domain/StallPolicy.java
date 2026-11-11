package com.salonhub.onboarding.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

// Decides when a salon has been in its current stage too long and needs a follow-up.
public final class StallPolicy {

    public static Map<OnboardingStage, Duration> defaultThresholds() {
        Map<OnboardingStage, Duration> t = new EnumMap<>(OnboardingStage.class);
        t.put(OnboardingStage.NOT_STARTED, Duration.ofDays(3));
        t.put(OnboardingStage.SIGNUP_IN_PROGRESS, Duration.ofDays(1));
        t.put(OnboardingStage.SIGNUP_ABANDONED, Duration.ofDays(1));
        t.put(OnboardingStage.SIGNUP_ERROR, Duration.ZERO);
        t.put(OnboardingStage.CONNECTION_INCOMPLETE, Duration.ZERO);
        // Meta requires registration within 14 days of signup; chase well before that.
        t.put(OnboardingStage.NUMBER_PENDING, Duration.ofDays(2));
        t.put(OnboardingStage.VERIFICATION_NOT_STARTED, Duration.ofDays(5));
        t.put(OnboardingStage.VERIFICATION_STARTED, Duration.ofDays(3));
        t.put(OnboardingStage.VERIFICATION_IN_REVIEW, Duration.ofDays(10));
        t.put(OnboardingStage.VERIFICATION_NEEDS_INFO, Duration.ofDays(3));
        t.put(OnboardingStage.VERIFICATION_FAILED, Duration.ZERO);
        t.put(OnboardingStage.VERIFICATION_UNKNOWN, Duration.ofDays(7));
        // No entry for LIVE: a live salon is never stalled.
        return t;
    }

    public record Assessment(boolean stalled, Duration timeInStage, Duration threshold) {
    }

    private final Map<OnboardingStage, Duration> thresholds;

    public StallPolicy() {
        this(Map.of());
    }

    public StallPolicy(Map<OnboardingStage, Duration> overrides) {
        Map<OnboardingStage, Duration> merged = defaultThresholds();
        if (overrides != null) {
            merged.putAll(overrides);
        }
        this.thresholds = Collections.unmodifiableMap(merged);
    }

    public Map<OnboardingStage, Duration> thresholds() {
        return thresholds;
    }

    public Assessment assess(OnboardingStage stage, Instant stageSince, Instant now) {
        Duration inStage = (stageSince == null || stageSince.isAfter(now))
                ? Duration.ZERO
                : Duration.between(stageSince, now);
        Duration threshold = thresholds.get(stage);
        boolean stalled = threshold != null && inStage.compareTo(threshold) >= 0;
        return new Assessment(stalled, inStage, threshold);
    }
}
