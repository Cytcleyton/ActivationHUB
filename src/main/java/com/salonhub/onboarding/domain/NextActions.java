package com.salonhub.onboarding.domain;

import java.util.Map;

// Plain-English guidance for the onboarding team, per stage.
public final class NextActions {

    // Meta's current_step values for the screens of the Embedded Signup pop-up.
    private static final Map<String, String> SIGNUP_STEPS = Map.of(
            "BUSINESS_ACCOUNT_SELECTION", "business portfolio",
            "WABA_PHONE_PROFILE_PICKER", "WhatsApp account selection",
            "WHATSAPP_BUSINESS_PROFILE_SETUP", "WhatsApp account creation",
            "PHONE_NUMBER_SETUP", "phone number entry",
            "PHONE_NUMBER_VERIFICATION", "phone number verification",
            "PERMISSIONS", "permissions review");

    private NextActions() {
    }

    public static String signupStepLabel(String currentStep) {
        if (currentStep == null || currentStep.isBlank()) {
            return null;
        }
        return SIGNUP_STEPS.getOrDefault(currentStep, currentStep.toLowerCase().replace('_', ' '));
    }

    // " at the phone number verification screen", or "" when Meta didn't say which screen.
    public static String whereAbandoned(String currentStep) {
        String label = signupStepLabel(currentStep);
        return label == null ? "" : " at the " + label + " screen";
    }

    public static String forStage(OnboardingStage stage, Salon salon) {
        return switch (stage) {
            case NOT_STARTED -> "Remind the salon to open Connect WhatsApp and log in with their Facebook account.";
            case SIGNUP_IN_PROGRESS -> "The salon opened Meta's pop-up but nothing has come back yet. "
                    + "If it stays like this, they probably closed the browser: call them.";
            case SIGNUP_ABANDONED -> "Call the salon: they closed Meta's pop-up" + whereAbandoned(salon.getSignupAbandonedStep())
                    + ". Talk them through it while they reopen Connect WhatsApp.";
            case SIGNUP_ERROR -> "The salon hit an error in Meta's pop-up"
                    + (salon.getSignupError() == null ? "" : " (" + salon.getSignupError() + ")")
                    + ". Help them fix it and reopen Connect WhatsApp.";
            case CONNECTION_INCOMPLETE -> "The salon finished Meta's pop-up but we couldn't complete the connection. "
                    + "Check the activity log.";
            case NUMBER_PENDING -> "Register the salon's number. Meta requires this within 14 days of signup.";
            case VERIFICATION_NOT_STARTED -> "Ask the salon to start business verification in Meta Business Suite. "
                    + "Have their registration documents ready.";
            case VERIFICATION_STARTED -> "The salon opened Meta's verification but hasn't submitted anything. "
                    + "Call them: it's usually finding the right registration document or matching the legal name.";
            case VERIFICATION_IN_REVIEW -> "Nothing to do: Meta is reviewing. Check in if it's taking more than "
                    + "a couple of weeks.";
            case VERIFICATION_NEEDS_INFO -> "Meta wants more information. Call the salon and help them respond in "
                    + "Meta Business Suite.";
            case VERIFICATION_FAILED -> "Verification failed. Go through Meta's reason with the salon, fix the "
                    + "details (often the legal name) and resubmit.";
            case VERIFICATION_UNKNOWN -> "We couldn't read the verification status. Ask the salon to look in "
                    + "Meta Business Suite.";
            case LIVE -> "Nothing to do: the salon is live.";
        };
    }
}
