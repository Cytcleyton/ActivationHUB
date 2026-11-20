package com.salonhub.onboarding.service;

// What Meta's Embedded Signup pop-up reports back, forwarded by the browser.
// event is FINISH (or another FINISH_ value), CANCEL or ERROR, plus our own CLOSED when the pop-up
// closed without Meta saying anything. currentStep is the screen a cancelled signup stopped on.
public record SignupSessionEvent(String event,
                                 String currentStep,
                                 String wabaId,
                                 String phoneNumberId,
                                 String businessId,
                                 String errorMessage,
                                 String errorCode) {

    public boolean isFinish() {
        return event != null && event.startsWith("FINISH");
    }

    public boolean isReportedError() {
        return "ERROR".equals(event) || (errorMessage != null && !errorMessage.isBlank());
    }

    public boolean isCancel() {
        return "CANCEL".equals(event) || "CLOSED".equals(event);
    }
}
