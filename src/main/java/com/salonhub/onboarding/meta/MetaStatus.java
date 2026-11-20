package com.salonhub.onboarding.meta;

// Statuses read from Meta. Either can be null when Meta doesn't say.
public record MetaStatus(String phoneStatus, String businessVerificationStatus) {
}
