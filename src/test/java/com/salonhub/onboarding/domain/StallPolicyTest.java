package com.salonhub.onboarding.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StallPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");
    private final StallPolicy policy = new StallPolicy();

    private static Instant ago(Duration d) {
        return NOW.minus(d);
    }

    @Test
    void notStalledBeforeTheThreshold() {
        var a = policy.assess(OnboardingStage.NOT_STARTED, ago(Duration.ofDays(2)), NOW);
        assertThat(a.stalled()).isFalse();
        assertThat(a.timeInStage()).isEqualTo(Duration.ofDays(2));
        assertThat(a.threshold()).isEqualTo(Duration.ofDays(3));
    }

    @Test
    void stalledOnceTheThresholdIsReached() {
        assertThat(policy.assess(OnboardingStage.NOT_STARTED, ago(Duration.ofDays(3)), NOW).stalled()).isTrue();
        assertThat(policy.assess(OnboardingStage.SIGNUP_ABANDONED, ago(Duration.ofHours(25)), NOW).stalled()).isTrue();
    }

    @Test
    void zeroThresholdStagesStallStraightAway() {
        assertThat(policy.assess(OnboardingStage.VERIFICATION_FAILED, NOW, NOW).stalled()).isTrue();
        assertThat(policy.assess(OnboardingStage.SIGNUP_ERROR, NOW, NOW).stalled()).isTrue();
        assertThat(policy.assess(OnboardingStage.CONNECTION_INCOMPLETE, NOW, NOW).stalled()).isTrue();
    }

    @Test
    void liveNeverStalls() {
        var a = policy.assess(OnboardingStage.LIVE, ago(Duration.ofDays(365)), NOW);
        assertThat(a.stalled()).isFalse();
        assertThat(a.threshold()).isNull();
    }

    @Test
    void reviewByMetaGetsLonger() {
        assertThat(policy.assess(OnboardingStage.VERIFICATION_IN_REVIEW, ago(Duration.ofDays(9)), NOW).stalled()).isFalse();
        assertThat(policy.assess(OnboardingStage.VERIFICATION_IN_REVIEW, ago(Duration.ofDays(10)), NOW).stalled()).isTrue();
    }

    @Test
    void overridesReplaceOnlyTheGivenStages() {
        StallPolicy custom = new StallPolicy(Map.of(OnboardingStage.VERIFICATION_STARTED, Duration.ofMinutes(2)));

        assertThat(custom.assess(OnboardingStage.VERIFICATION_STARTED, ago(Duration.ofMinutes(3)), NOW).stalled()).isTrue();
        assertThat(custom.thresholds().get(OnboardingStage.NOT_STARTED)).isEqualTo(Duration.ofDays(3));
    }

    @Test
    void everyStageExceptLiveHasAThreshold() {
        for (OnboardingStage stage : OnboardingStage.values()) {
            assertThat(policy.thresholds().containsKey(stage)).as(stage.name()).isEqualTo(stage != OnboardingStage.LIVE);
        }
    }

    @Test
    void aStartInTheFutureCountsAsZero() {
        var a = policy.assess(OnboardingStage.NOT_STARTED, NOW.plusSeconds(60), NOW);
        assertThat(a.timeInStage()).isZero();
        assertThat(a.stalled()).isFalse();
    }
}
