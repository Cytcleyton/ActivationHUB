package com.salonhub.onboarding.web;

import com.salonhub.onboarding.domain.FeatureType;
import com.salonhub.onboarding.domain.OnboardingStage;
import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.domain.SalonFeature;
import com.salonhub.onboarding.domain.StallPolicy;
import com.salonhub.onboarding.web.Dtos.FeatureChip;
import com.salonhub.onboarding.web.Dtos.FeatureView;
import com.salonhub.onboarding.web.Dtos.Overview;
import com.salonhub.onboarding.web.Dtos.OverviewRow;
import com.salonhub.onboarding.web.Dtos.OverviewStats;
import com.salonhub.onboarding.web.Dtos.SalonDetail;
import com.salonhub.onboarding.web.Dtos.SalonSummary;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Turns entities into the JSON the pages need.
@Component
class SalonViews {

    private final StallPolicy stallPolicy;
    private final Clock clock;

    SalonViews(StallPolicy stallPolicy, Clock clock) {
        this.stallPolicy = stallPolicy;
        this.clock = clock;
    }

    // A feature's status: WhatsApp reminders follows the onboarding stage, demo-only features are live.
    private record Status(String label, String tone, OnboardingStage stage, StallPolicy.Assessment assessment) {
    }

    private Status status(Salon salon, FeatureType type, Instant now) {
        if (!type.implemented()) {
            return new Status("Live", "done", null, null);
        }
        OnboardingStage stage = salon.getStage();
        return new Status(stage.label(), stage.tone().css(), stage,
                stallPolicy.assess(stage, salon.getStageSince(), now));
    }

    SalonSummary summary(Salon salon, List<SalonFeature> features) {
        Instant now = clock.instant();
        List<FeatureChip> chips = features.stream()
                .map(f -> {
                    Status s = status(salon, f.getType(), now);
                    return new FeatureChip(f.getType().name(), f.getType().label(), s.label(), s.tone());
                })
                .toList();
        return new SalonSummary(salon.getId(), salon.getName(), salon.isDemo(), salon.getCreatedAt(), chips);
    }

    // Lists every feature type, so the page can offer "Add" for the ones not added yet.
    SalonDetail detail(Salon salon, List<SalonFeature> features) {
        Instant now = clock.instant();
        Map<FeatureType, SalonFeature> added = features.stream()
                .collect(Collectors.toMap(SalonFeature::getType, Function.identity()));
        List<FeatureView> views = Arrays.stream(FeatureType.values())
                .map(type -> {
                    SalonFeature f = added.get(type);
                    Status s = f == null ? null : status(salon, type, now);
                    return new FeatureView(type.name(), type.label(), type.description(), type.implemented(),
                            f != null, f == null ? null : f.getAddedAt(),
                            s == null ? null : s.label(), s == null ? null : s.tone());
                })
                .toList();
        return new SalonDetail(salon.getId(), salon.getName(), salon.isDemo(), salon.getCity(),
                salon.getContactName(), salon.getCreatedAt(), views);
    }

    // Newest first; salons without features don't appear.
    Overview overview(List<Salon> salons, Map<Long, List<SalonFeature>> featuresBySalon) {
        Instant now = clock.instant();
        Map<Long, Salon> salonsById = salons.stream()
                .collect(Collectors.toMap(Salon::getId, Function.identity()));
        Comparator<SalonFeature> newestFirst = Comparator.comparing(SalonFeature::getAddedAt)
                .thenComparing(SalonFeature::getId)
                .reversed();
        List<OverviewRow> rows = featuresBySalon.values().stream()
                .flatMap(List::stream)
                .filter(f -> salonsById.containsKey(f.getSalonId()))
                .sorted(newestFirst)
                .map(f -> overviewRow(salonsById.get(f.getSalonId()), f, now))
                .toList();
        OverviewStats stats = new OverviewStats(
                rows.stream().filter(r -> r.stage() != null).count(),
                rows.stream().filter(OverviewRow::stalled).count(),
                rows.stream().filter(r -> "done".equals(r.statusTone())).count());
        return new Overview(rows, stats);
    }

    private OverviewRow overviewRow(Salon salon, SalonFeature feature, Instant now) {
        FeatureType type = feature.getType();
        Status s = status(salon, type, now);
        StallPolicy.Assessment a = s.assessment();
        return new OverviewRow(salon.getId(), salon.getName(), salon.isDemo(), salon.getCity(), salon.getContactName(),
                type.name(), type.label(), s.label(), s.tone(),
                s.stage() == null ? null : s.stage().name(),
                s.stage() == null ? null : s.stage().waitingOn().label(),
                a != null && a.stalled(),
                a == null ? null : humanDuration(a.timeInStage()),
                a == null ? 0 : a.timeInStage().toSeconds(),
                feature.getAddedAt());
    }

    // "3d 4h", "5h 12m", "12m" or "under a minute".
    static String humanDuration(Duration d) {
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        if (days > 0) return hours > 0 ? days + "d " + hours + "h" : days + "d";
        if (hours > 0) return minutes > 0 ? hours + "h " + minutes + "m" : hours + "h";
        if (minutes > 0) return minutes + "m";
        return "under a minute";
    }
}
