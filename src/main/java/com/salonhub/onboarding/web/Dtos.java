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

    public record SalonSummary(long id, String name, boolean demo, Instant createdAt, List<FeatureChip> features) {
    }

    public record SalonList(List<SalonSummary> salons) {
    }

    public record FeatureView(String type, String label, String description, boolean implemented,
                              boolean added, Instant addedAt, String statusLabel, String statusTone) {
    }

    public record SalonDetail(long id, String name, boolean demo, String city, String contactName,
                              Instant createdAt, List<FeatureView> features) {
    }

    // One row per feature added to a salon. stage, waitingOn and timeInStage are only set for WhatsApp reminders.
    public record OverviewRow(long salonId, String salonName, boolean demo, String city, String contactName,
                              String featureType, String featureLabel, String statusLabel, String statusTone,
                              String stage, String waitingOn, boolean stalled, String timeInStage,
                              long secondsInStage, Instant addedAt) {
    }

    public record OverviewStats(long onboarding, long stalled, long live) {
    }

    public record Overview(List<OverviewRow> rows, OverviewStats stats) {
    }

    public record ErrorResponse(int status, String message) {
    }
}
