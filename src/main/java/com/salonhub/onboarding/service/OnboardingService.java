package com.salonhub.onboarding.service;

import com.salonhub.onboarding.domain.FeatureType;
import com.salonhub.onboarding.domain.OnboardingStage;
import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.domain.SalonFeature;
import com.salonhub.onboarding.domain.StageCalculator;
import com.salonhub.onboarding.repo.SalonFeatureRepository;
import com.salonhub.onboarding.repo.SalonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class OnboardingService {

    private final SalonRepository salons;
    private final SalonFeatureRepository features;
    private final Clock clock;

    public OnboardingService(SalonRepository salons, SalonFeatureRepository features, Clock clock) {
        this.salons = salons;
        this.features = features;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Salon> listSalons() {
        return salons.findAllByOrderByCreatedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Salon getSalon(long id) {
        return salons.findById(id).orElseThrow(() -> OnboardingException.notFound("Salon not found."));
    }

    public Salon createSalon(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw OnboardingException.badRequest("Enter the salon's name.");
        }
        if (trimmed.length() > Salon.MAX_NAME_LENGTH) {
            throw OnboardingException.badRequest(
                    "The salon's name can be at most " + Salon.MAX_NAME_LENGTH + " characters.");
        }
        return salons.save(new Salon(trimmed, clock.instant()));
    }

    public void deleteSalon(long id) {
        Salon salon = getSalon(id);
        features.deleteBySalonId(salon.getId());
        salons.delete(salon);
    }

    @Transactional(readOnly = true)
    public List<SalonFeature> featuresOf(long salonId) {
        return features.findBySalonIdOrderByAddedAtAscIdAsc(salonId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<SalonFeature>> featuresBySalon() {
        return features.findAllByOrderByAddedAtAscIdAsc().stream()
                .collect(Collectors.groupingBy(SalonFeature::getSalonId));
    }

    // Adding a feature the salon already has does nothing.
    public void addFeature(long salonId, String type) {
        Salon salon = getSalon(salonId);
        FeatureType featureType = FeatureType.parse(type)
                .orElseThrow(() -> OnboardingException.badRequest("Unknown feature: " + type));
        if (features.existsBySalonIdAndType(salon.getId(), featureType)) {
            return;
        }
        Instant now = clock.instant();
        features.save(new SalonFeature(salon.getId(), featureType, now));
        if (featureType == FeatureType.WHATSAPP_REMINDERS) {
            // Onboarding starts now, so time in stage counts from here.
            salon.setStageSince(now);
            updateStage(salon);
        }
    }

    // Call after anything that could move the salon to another stage.
    void updateStage(Salon salon) {
        OnboardingStage next = StageCalculator.calculate(salon.stageInputs());
        if (next != salon.getStage()) {
            salon.setStage(next);
            salon.setStageSince(clock.instant());
        }
    }
}
