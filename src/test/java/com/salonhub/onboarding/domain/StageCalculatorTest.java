package com.salonhub.onboarding.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StageCalculatorTest {

    private static StageCalculator.Inputs connected(String phoneStatus, String verification) {
        return new StageCalculator.Inputs(SignupState.FINISHED, true, "waba-1", false, phoneStatus, verification);
    }

    @Test
    void signupStatesBeforeFinishing() {
        assertThat(stageFor(SignupState.NOT_STARTED)).isEqualTo(OnboardingStage.NOT_STARTED);
        assertThat(stageFor(SignupState.IN_PROGRESS)).isEqualTo(OnboardingStage.SIGNUP_IN_PROGRESS);
        assertThat(stageFor(SignupState.ABANDONED)).isEqualTo(OnboardingStage.SIGNUP_ABANDONED);
        assertThat(stageFor(SignupState.ERROR)).isEqualTo(OnboardingStage.SIGNUP_ERROR);
    }

    private static OnboardingStage stageFor(SignupState state) {
        return StageCalculator.calculate(new StageCalculator.Inputs(state, false, null, false, null, null));
    }

    @Test
    void finishedWithoutTokenOrWhatsAppAccountIsIncomplete() {
        assertThat(StageCalculator.calculate(new StageCalculator.Inputs(
                SignupState.FINISHED, false, "waba-1", false, "CONNECTED", "verified")))
                .isEqualTo(OnboardingStage.CONNECTION_INCOMPLETE);
        assertThat(StageCalculator.calculate(new StageCalculator.Inputs(
                SignupState.FINISHED, true, " ", false, "CONNECTED", "verified")))
                .isEqualTo(OnboardingStage.CONNECTION_INCOMPLETE);
    }

    @Test
    void phoneNotConnectedMeansNumberPending() {
        assertThat(StageCalculator.calculate(connected("PENDING", "verified"))).isEqualTo(OnboardingStage.NUMBER_PENDING);
    }

    @Test
    void withoutAPhoneStatusOurOwnRegistrationCounts() {
        assertThat(StageCalculator.calculate(connected(null, "not_verified"))).isEqualTo(OnboardingStage.NUMBER_PENDING);
        assertThat(StageCalculator.calculate(new StageCalculator.Inputs(
                SignupState.FINISHED, true, "waba-1", true, null, "not_verified")))
                .isEqualTo(OnboardingStage.VERIFICATION_NOT_STARTED);
    }

    @ParameterizedTest
    @CsvSource({
            "not_verified, VERIFICATION_NOT_STARTED",
            "pending_submission, VERIFICATION_STARTED",
            "pending, VERIFICATION_IN_REVIEW",
            "pending_need_more_info, VERIFICATION_NEEDS_INFO",
            "verified, LIVE",
            "rejected, VERIFICATION_FAILED",
            "failed, VERIFICATION_FAILED",
            "expired, VERIFICATION_FAILED",
            "revoked, VERIFICATION_FAILED",
            "ineligible, VERIFICATION_FAILED",
            "something_new, VERIFICATION_UNKNOWN",
            "' VERIFIED ', LIVE"
    })
    void everyMetaVerificationValue(String metaValue, OnboardingStage expected) {
        assertThat(StageCalculator.calculate(connected("CONNECTED", metaValue))).isEqualTo(expected);
    }

    @Test
    void missingVerificationStatusIsUnknown() {
        assertThat(StageCalculator.calculate(connected("CONNECTED", null))).isEqualTo(OnboardingStage.VERIFICATION_UNKNOWN);
        assertThat(StageCalculator.calculate(connected("connected", ""))).isEqualTo(OnboardingStage.VERIFICATION_UNKNOWN);
    }
}
