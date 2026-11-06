package com.salonhub.onboarding.web;

import java.time.Instant;
import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    public record CreateSalonRequest(String name) {
    }

    // tone is one of: done, waiting, problem, neutral (matches the CSS pill classes)
    public record FeatureChip(String type, String label, String statusLabel, String statusTone) {
    }

    public record SalonSummary(long id, String name, Instant createdAt, List<FeatureChip> features) {
    }

    public record SalonList(List<SalonSummary> salons) {
    }

    public record FeatureView(String type, String label, String description, boolean implemented,
                              boolean added, Instant addedAt, String statusLabel, String statusTone) {
    }

    public record SalonDetail(long id, String name, Instant createdAt, List<FeatureView> features) {
    }

    public record ErrorResponse(int status, String message) {
    }
}
