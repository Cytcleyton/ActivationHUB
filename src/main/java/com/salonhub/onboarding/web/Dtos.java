package com.salonhub.onboarding.web;

import com.salonhub.onboarding.domain.Salon;

import java.time.Instant;
import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    public record CreateSalonRequest(String name) {
    }

    public record SalonSummary(long id, String name, Instant createdAt) {
        static SalonSummary of(Salon salon) {
            return new SalonSummary(salon.getId(), salon.getName(), salon.getCreatedAt());
        }
    }

    public record SalonList(List<SalonSummary> salons) {
    }

    public record ErrorResponse(int status, String message) {
    }
}
