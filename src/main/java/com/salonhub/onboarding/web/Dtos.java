package com.salonhub.onboarding.web;

import java.time.Instant;
import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    public record CreateSalonRequest(String name) {
    }

    // redirectUri is the page that opened the pop-up; dialogRedirectUris are the addresses
    // the browser saw Meta's dialog use. Meta only accepts the code with the same address.
    public record SignupCodeRequest(String code, String redirectUri, List<String> dialogRedirectUris) {
    }

    public record SimulateVerificationRequest(String status) {
    }

    public record SkipAheadRequest(int days) {
    }

    // Only values that are safe in the browser: never the app secret or any token.
    public record ConfigResponse(String mode, String appId, String configId, String graphVersion,
                                 boolean liveSignupReady) {
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

    // The WhatsApp reminders onboarding, shown on the salon page once the feature is added.
    public record WhatsAppView(String stage, String stageLabel, String stageTone, String waitingOn,
                               boolean stalled, String timeInStage, String stalledAfter, String nextAction,
                               String signupState, String abandonedAt, String signupError,
                               boolean connected, boolean phoneConnected,
                               String verificationLabel, String verificationTone,
                               Instant lastCheckedAt, String lastCheckError,
                               String simulatedVerification) {
    }

    public record EventView(Instant at, String type, String detail) {
    }

    public record SalonDetail(long id, String name, boolean demo, String city, String contactName,
                              Instant createdAt, List<FeatureView> features, WhatsAppView whatsapp,
                              List<EventView> events) {
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
