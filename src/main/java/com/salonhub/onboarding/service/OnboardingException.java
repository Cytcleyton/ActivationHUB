package com.salonhub.onboarding.service;

public class OnboardingException extends RuntimeException {

    private final int status;

    private OnboardingException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static OnboardingException badRequest(String message) {
        return new OnboardingException(400, message);
    }

    public static OnboardingException notFound(String message) {
        return new OnboardingException(404, message);
    }

    public int getStatus() {
        return status;
    }
}
