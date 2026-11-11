package com.salonhub.onboarding.service;

import com.salonhub.onboarding.config.AppProperties;
import com.salonhub.onboarding.domain.FeatureType;
import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.domain.SalonFeature;
import com.salonhub.onboarding.domain.SalonSource;
import com.salonhub.onboarding.domain.SignupState;
import com.salonhub.onboarding.domain.StageCalculator;
import com.salonhub.onboarding.repo.SalonFeatureRepository;
import com.salonhub.onboarding.repo.SalonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Seeds eight DEMO salons in different stages on first start, so the overview has something to show.
@Component
public class StartupData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupData.class);

    private final SalonRepository salons;
    private final SalonFeatureRepository features;
    private final AppProperties appProperties;
    private final Clock clock;

    public StartupData(SalonRepository salons, SalonFeatureRepository features,
                       AppProperties appProperties, Clock clock) {
        this.salons = salons;
        this.features = features;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (appProperties.isSeedDemoData() && salons.countBySource(SalonSource.DEMO) == 0) {
            log.info("Seeding demo salons");
            seedDemoSalons(clock.instant());
        }
    }

    private void seedDemoSalons(Instant now) {
        // Rejected by Meta 12 days ago (name mismatch): needs a call.
        demo(now, new Demo("Glow Hair Studio", "Cork", "Aoife", 30, SignupState.FINISHED, null, true,
                "rejected", "CONNECTED", 12, List.of(FeatureType.ADS_MANAGER)));

        // Meta asked for more information 8 days ago and the salon hasn't replied.
        demo(now, new Demo("The Barber Room", "Galway", "Liam", 21, SignupState.FINISHED, null, true,
                "pending_need_more_info", "CONNECTED", 8, List.of()));

        // Closed Meta's pop-up at phone verification 4 days ago.
        demo(now, new Demo("Bloom Beauty", "Leeds", "Aoife", 9, SignupState.ABANDONED, "PHONE_NUMBER_VERIFICATION",
                false, null, null, 4, List.of()));

        // In Meta's review for 2 days: fine for now.
        demo(now, new Demo("Salon Nua", "Dublin", "Liam", 12, SignupState.FINISHED, null, true,
                "pending", "CONNECTED", 2, List.of(FeatureType.BRANDED_APP)));

        // Finished signup 3 days ago but nobody registered the number.
        demo(now, new Demo("Lush Lashes", "Belfast", "Aoife", 5, SignupState.FINISHED, null, false,
                "not_verified", "PENDING", 3, List.of()));

        // Feature added 6 days ago; the salon never opened Meta's pop-up.
        demo(now, new Demo("Fade Factory", "Limerick", "Liam", 6, SignupState.NOT_STARTED, null, false,
                null, null, 6, List.of()));

        // Connected yesterday, verification not started yet: fine for now.
        demo(now, new Demo("Bella Nails", "Waterford", "Aoife", 3, SignupState.FINISHED, null, true,
                "not_verified", "CONNECTED", 1, List.of()));

        // Fully live.
        demo(now, new Demo("Halo Hair Co.", "Manchester", "Liam", 25, SignupState.FINISHED, null, true,
                "verified", "CONNECTED", 1, List.of(FeatureType.BRANDED_APP, FeatureType.ADS_MANAGER)));
    }

    private record Demo(String name, String city, String contact, int createdDaysAgo,
                        SignupState signup, String abandonedStep, boolean registered,
                        String verificationStatus, String phoneStatus, int daysInStage,
                        List<FeatureType> extraFeatures) {
    }

    private void demo(Instant now, Demo d) {
        Instant created = now.minus(Duration.ofDays(d.createdDaysAgo()));
        Salon salon = new Salon(d.name(), created);
        salon.setSource(SalonSource.DEMO);
        salon.setCity(d.city());
        salon.setContactName(d.contact());
        salon.setSignupState(d.signup());
        salon.setSignupAbandonedStep(d.abandonedStep());
        if (d.signup() == SignupState.FINISHED) {
            String slug = d.name().toLowerCase().replaceAll("[^a-z]", "");
            salon.setBusinessId("demo-business-" + slug);
            salon.setWabaId("demo-waba-" + slug);
            salon.setPhoneNumberId("demo-phone-" + slug);
            salon.setAccessToken("demo-token-not-used");
            salon.setNumberRegistered(d.registered());
        }
        salon.setPhoneStatus(d.phoneStatus());
        salon.setBusinessVerificationStatus(d.verificationStatus());
        salon.setStage(StageCalculator.calculate(salon.stageInputs()));
        // The extra 3 hours make "time in stage" look less round.
        salon.setStageSince(now.minus(Duration.ofDays(d.daysInStage())).minus(Duration.ofHours(3)));
        salon = salons.save(salon);

        features.save(new SalonFeature(salon.getId(), FeatureType.WHATSAPP_REMINDERS, created));
        for (FeatureType extra : d.extraFeatures()) {
            features.save(new SalonFeature(salon.getId(), extra, created));
        }
    }
}
