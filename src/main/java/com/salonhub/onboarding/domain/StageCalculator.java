package com.salonhub.onboarding.domain;

import java.util.Locale;
import java.util.Set;

// Works out a salon's stage from its signup and the statuses Meta reports. No Spring, so it's easy to test.
public final class StageCalculator {

    private static final Set<String> FAILED = Set.of("rejected", "failed", "expired", "revoked", "ineligible");

    private StageCalculator() {
    }

    public record Inputs(SignupState signupState,
                         boolean hasToken,
                         String wabaId,
                         boolean numberRegistered,
                         String phoneStatus,
                         String businessVerificationStatus) {
    }

    public static OnboardingStage calculate(Inputs in) {
        switch (in.signupState()) {
            case NOT_STARTED: return OnboardingStage.NOT_STARTED;
            case IN_PROGRESS: return OnboardingStage.SIGNUP_IN_PROGRESS;
            case ABANDONED: return OnboardingStage.SIGNUP_ABANDONED;
            case ERROR: return OnboardingStage.SIGNUP_ERROR;
            default: break;
        }
        if (!in.hasToken() || isBlank(in.wabaId())) {
            return OnboardingStage.CONNECTION_INCOMPLETE;
        }
        if (!isPhoneConnected(in.phoneStatus(), in.numberRegistered())) {
            return OnboardingStage.NUMBER_PENDING;
        }
        return verificationStage(in.businessVerificationStatus());
    }

    public static boolean isPhoneConnected(String phoneStatus, boolean numberRegistered) {
        if (isBlank(phoneStatus)) {
            // Meta didn't say; trust our own successful register call.
            return numberRegistered;
        }
        return "CONNECTED".equalsIgnoreCase(phoneStatus.trim());
    }

    // Maps Meta's business verification_status onto a stage.
    public static OnboardingStage verificationStage(String businessVerificationStatus) {
        if (isBlank(businessVerificationStatus)) {
            return OnboardingStage.VERIFICATION_UNKNOWN;
        }
        String status = businessVerificationStatus.trim().toLowerCase(Locale.ROOT);
        if (FAILED.contains(status)) {
            return OnboardingStage.VERIFICATION_FAILED;
        }
        return switch (status) {
            case "verified" -> OnboardingStage.LIVE;
            case "pending" -> OnboardingStage.VERIFICATION_IN_REVIEW;
            case "pending_need_more_info" -> OnboardingStage.VERIFICATION_NEEDS_INFO;
            case "pending_submission" -> OnboardingStage.VERIFICATION_STARTED;
            case "not_verified" -> OnboardingStage.VERIFICATION_NOT_STARTED;
            default -> OnboardingStage.VERIFICATION_UNKNOWN;
        };
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
