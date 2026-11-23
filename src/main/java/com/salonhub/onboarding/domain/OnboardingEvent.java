package com.salonhub.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

// One line in a salon's activity log.
@Entity
public class OnboardingEvent {

    public enum Type {
        SALON_CREATED, FEATURE_ADDED, STAGE_CHANGED, STATUS_CHANGED,
        SIGNUP_LAUNCHED, SIGNUP_ABANDONED, SIGNUP_ERROR, SIGNUP_FINISHED, NUMBER_REGISTERED,
        VERIFICATION_OPENED, DEMO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long salonId;

    @Column(nullable = false)
    private Instant occurredAt;

    // Plain text rather than a database enum, so adding an event type doesn't need a database change.
    @Column(nullable = false, length = 40)
    private String type;

    @Column(length = 1000)
    private String detail;

    protected OnboardingEvent() {
    }

    public OnboardingEvent(Long salonId, Instant occurredAt, Type type, String detail) {
        this.salonId = salonId;
        this.occurredAt = occurredAt;
        this.type = type.name();
        this.detail = detail;
    }

    public Long getId() { return id; }
    public Long getSalonId() { return salonId; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Type getType() { return Type.valueOf(type); }
    public String getDetail() { return detail; }
}
