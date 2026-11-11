package com.salonhub.onboarding.domain;

// Where a salon is in WhatsApp onboarding, and who has to act next.
public enum OnboardingStage {

    NOT_STARTED("Waiting for salon to connect", WaitingOn.SALON, Tone.WAITING),
    SIGNUP_IN_PROGRESS("Meta signup opened", WaitingOn.SALON, Tone.WAITING),
    SIGNUP_ABANDONED("Abandoned Meta signup", WaitingOn.SALON, Tone.PROBLEM),
    SIGNUP_ERROR("Error during Meta signup", WaitingOn.SALON, Tone.PROBLEM),
    CONNECTION_INCOMPLETE("Connection incomplete", WaitingOn.ONBOARDING_TEAM, Tone.PROBLEM),
    NUMBER_PENDING("Number not registered", WaitingOn.ONBOARDING_TEAM, Tone.WAITING),
    VERIFICATION_NOT_STARTED("Business verification not started", WaitingOn.SALON, Tone.WAITING),
    VERIFICATION_STARTED("Verification started, not submitted", WaitingOn.SALON, Tone.WAITING),
    VERIFICATION_IN_REVIEW("Meta reviewing business", WaitingOn.META, Tone.WAITING),
    VERIFICATION_NEEDS_INFO("Meta needs more information", WaitingOn.SALON, Tone.WAITING),
    VERIFICATION_FAILED("Business verification failed", WaitingOn.SALON, Tone.PROBLEM),
    VERIFICATION_UNKNOWN("Verification status unavailable", WaitingOn.ONBOARDING_TEAM, Tone.NEUTRAL),
    LIVE("Live", WaitingOn.NOBODY, Tone.DONE);

    public enum WaitingOn {
        SALON("Salon"), META("Meta"), ONBOARDING_TEAM("Onboarding team"), NOBODY("—");

        private final String label;

        WaitingOn(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    // Colour family in the UI: green, amber, red or grey.
    public enum Tone {
        DONE, WAITING, PROBLEM, NEUTRAL;

        public String css() {
            return name().toLowerCase();
        }
    }

    private final String label;
    private final WaitingOn waitingOn;
    private final Tone tone;

    OnboardingStage(String label, WaitingOn waitingOn, Tone tone) {
        this.label = label;
        this.waitingOn = waitingOn;
        this.tone = tone;
    }

    public String label() { return label; }
    public WaitingOn waitingOn() { return waitingOn; }
    public Tone tone() { return tone; }
}
