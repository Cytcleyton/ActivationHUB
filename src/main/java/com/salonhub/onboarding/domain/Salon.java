package com.salonhub.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class Salon {

    public static final int MAX_NAME_LENGTH = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false)
    private Instant createdAt;

    // Only filled in for the demo salons.
    private String city;
    private String contactName;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private SalonSource source = SalonSource.LIVE;

    // WhatsApp onboarding. Getters below default these for rows saved before the columns existed.
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private OnboardingStage stage = OnboardingStage.NOT_STARTED;
    private Instant stageSince;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SignupState signupState = SignupState.NOT_STARTED;
    private String signupAbandonedStep;
    @Column(length = 500)
    private String signupError;

    private String businessId;
    private String wabaId;
    private String phoneNumberId;
    @Column(length = 1000)
    private String accessToken;
    private Boolean numberRegistered = false;

    // Latest statuses read from Meta
    private String phoneStatus;
    private String businessVerificationStatus;

    protected Salon() {
    }

    public Salon(String name, Instant createdAt) {
        this.name = name;
        this.createdAt = createdAt;
        this.stageSince = createdAt;
    }

    public boolean isDemo() {
        return getSource() == SalonSource.DEMO;
    }

    public boolean hasToken() {
        return accessToken != null && !accessToken.isBlank();
    }

    public StageCalculator.Inputs stageInputs() {
        return new StageCalculator.Inputs(getSignupState(), hasToken(), wabaId, isNumberRegistered(),
                phoneStatus, businessVerificationStatus);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public SalonSource getSource() { return source == null ? SalonSource.LIVE : source; }
    public void setSource(SalonSource source) { this.source = source; }
    public OnboardingStage getStage() { return stage == null ? OnboardingStage.NOT_STARTED : stage; }
    public void setStage(OnboardingStage stage) { this.stage = stage; }
    public Instant getStageSince() { return stageSince == null ? createdAt : stageSince; }
    public void setStageSince(Instant stageSince) { this.stageSince = stageSince; }
    public SignupState getSignupState() { return signupState == null ? SignupState.NOT_STARTED : signupState; }
    public void setSignupState(SignupState signupState) { this.signupState = signupState; }
    public String getSignupAbandonedStep() { return signupAbandonedStep; }
    public void setSignupAbandonedStep(String signupAbandonedStep) { this.signupAbandonedStep = signupAbandonedStep; }
    public String getSignupError() { return signupError; }
    public void setSignupError(String signupError) { this.signupError = signupError; }
    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public String getWabaId() { return wabaId; }
    public void setWabaId(String wabaId) { this.wabaId = wabaId; }
    public String getPhoneNumberId() { return phoneNumberId; }
    public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public boolean isNumberRegistered() { return Boolean.TRUE.equals(numberRegistered); }
    public void setNumberRegistered(boolean numberRegistered) { this.numberRegistered = numberRegistered; }
    public String getPhoneStatus() { return phoneStatus; }
    public void setPhoneStatus(String phoneStatus) { this.phoneStatus = phoneStatus; }
    public String getBusinessVerificationStatus() { return businessVerificationStatus; }
    public void setBusinessVerificationStatus(String status) { this.businessVerificationStatus = status; }
}
