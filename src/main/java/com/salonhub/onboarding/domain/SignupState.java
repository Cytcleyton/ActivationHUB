package com.salonhub.onboarding.domain;

// What we know about the salon's run through Meta's Embedded Signup pop-up.
public enum SignupState {
    NOT_STARTED,
    IN_PROGRESS,
    ABANDONED,
    ERROR,
    FINISHED
}
