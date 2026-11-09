package com.salonhub.onboarding.web;

import com.salonhub.onboarding.domain.FeatureType;
import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.domain.SalonFeature;
import com.salonhub.onboarding.web.Dtos.FeatureChip;
import com.salonhub.onboarding.web.Dtos.FeatureView;
import com.salonhub.onboarding.web.Dtos.Overview;
import com.salonhub.onboarding.web.Dtos.OverviewRow;
import com.salonhub.onboarding.web.Dtos.SalonDetail;
import com.salonhub.onboarding.web.Dtos.SalonSummary;

import java.util.Arrays;
import java.util.Comparator;
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

    // Newest first; salons without features don't appear.
    static Overview overview(List<Salon> salons, Map<Long, List<SalonFeature>> featuresBySalon) {
        Map<Long, Salon> salonsById = salons.stream()
                .collect(Collectors.toMap(Salon::getId, Function.identity()));
        Comparator<SalonFeature> newestFirst = Comparator.comparing(SalonFeature::getAddedAt)
                .thenComparing(SalonFeature::getId)
                .reversed();
        List<OverviewRow> rows = featuresBySalon.values().stream()
                .flatMap(List::stream)
                .filter(f -> salonsById.containsKey(f.getSalonId()))
                .sorted(newestFirst)
                .map(f -> overviewRow(salonsById.get(f.getSalonId()), f))
                .toList();
        return new Overview(rows);
    }

    private static OverviewRow overviewRow(Salon salon, SalonFeature feature) {
        FeatureType type = feature.getType();
        return new OverviewRow(salon.getId(), salon.getName(), type.name(), type.label(),
                statusLabel(type), statusTone(type), feature.getAddedAt());
    }

    // WhatsApp reminders' real onboarding stages come in a later step.
    static String statusLabel(FeatureType type) {
        return type.implemented() ? "Waiting for salon to connect" : "Live";
    }

    static String statusTone(FeatureType type) {
        return type.implemented() ? "waiting" : "done";
    }
}
