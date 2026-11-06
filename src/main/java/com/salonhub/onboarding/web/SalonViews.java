package com.salonhub.onboarding.web;

import com.salonhub.onboarding.domain.FeatureType;
import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.domain.SalonFeature;
import com.salonhub.onboarding.web.Dtos.FeatureChip;
import com.salonhub.onboarding.web.Dtos.FeatureView;
import com.salonhub.onboarding.web.Dtos.SalonDetail;
import com.salonhub.onboarding.web.Dtos.SalonSummary;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class SalonViews {

    private SalonViews() {
    }

    static SalonSummary summary(Salon salon, List<SalonFeature> features) {
        List<FeatureChip> chips = features.stream()
                .map(f -> new FeatureChip(f.getType().name(), f.getType().label(),
                        statusLabel(f.getType()), statusTone(f.getType())))
                .toList();
        return new SalonSummary(salon.getId(), salon.getName(), salon.getCreatedAt(), chips);
    }

    // Lists every feature type, so the page can offer "Add" for the ones not added yet.
    static SalonDetail detail(Salon salon, List<SalonFeature> features) {
        Map<FeatureType, SalonFeature> added = features.stream()
                .collect(Collectors.toMap(SalonFeature::getType, Function.identity()));
        List<FeatureView> views = Arrays.stream(FeatureType.values())
                .map(type -> {
                    SalonFeature f = added.get(type);
                    return new FeatureView(type.name(), type.label(), type.description(), type.implemented(),
                            f != null, f == null ? null : f.getAddedAt(),
                            f == null ? null : statusLabel(type), f == null ? null : statusTone(type));
                })
                .toList();
        return new SalonDetail(salon.getId(), salon.getName(), salon.getCreatedAt(), views);
    }

    // WhatsApp reminders' real onboarding stages come in a later step.
    static String statusLabel(FeatureType type) {
        return type.implemented() ? "Waiting for salon to connect" : "Live";
    }

    static String statusTone(FeatureType type) {
        return type.implemented() ? "waiting" : "done";
    }
}
