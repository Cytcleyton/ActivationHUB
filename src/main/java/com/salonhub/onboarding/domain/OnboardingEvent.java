package com.salonhub.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

// One line in a salon's activity log.
@Entity
public class OnboardingEvent {

    public enum Type {
        SALON_CREATED, FEATURE_ADDED, STAGE_CHANGED, STATUS_CHANGED,
        SIGNUP_LAUNCHED, SIGNUP_ABANDONED, SIGNUP_ERROR, SIGNUP_FINISHED, NUMBER_REGISTERED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long salonId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Type type;

    @Column(length = 1000)
    private String detail;

    protected OnboardingEvent() {
    }

    public OnboardingEvent(Long salonId, Instant occurredAt, Type type, String detail) {
        this.salonId = salonId;
        this.occurredAt = occurredAt;
        this.type = type;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public Long getSalonId() { return salonId; }
    public Instant getOccurredAt() { return occurredAt; }
    public Type getType() { return type; }
    public String getDetail() { return detail; }
}
