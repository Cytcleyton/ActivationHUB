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
public class SalonFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long salonId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FeatureType type;

    @Column(nullable = false)
    private Instant addedAt;

    protected SalonFeature() {
    }

    public SalonFeature(Long salonId, FeatureType type, Instant addedAt) {
        this.salonId = salonId;
        this.type = type;
        this.addedAt = addedAt;
    }

    public Long getId() { return id; }
    public Long getSalonId() { return salonId; }
    public FeatureType getType() { return type; }
    public Instant getAddedAt() { return addedAt; }
}
