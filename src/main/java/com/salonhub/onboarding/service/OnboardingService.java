package com.salonhub.onboarding.service;

import com.salonhub.onboarding.config.MetaProperties;
import com.salonhub.onboarding.domain.FeatureType;
import com.salonhub.onboarding.domain.NextActions;
import com.salonhub.onboarding.domain.OnboardingEvent;
import com.salonhub.onboarding.domain.OnboardingEvent.Type;
import com.salonhub.onboarding.domain.OnboardingStage;
import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.domain.SalonFeature;
import com.salonhub.onboarding.domain.SignupState;
import com.salonhub.onboarding.domain.StageCalculator;
import com.salonhub.onboarding.meta.MetaApiException;
import com.salonhub.onboarding.meta.MetaAssets;
import com.salonhub.onboarding.meta.MetaClient;
import com.salonhub.onboarding.meta.MetaStatus;
import com.salonhub.onboarding.repo.OnboardingEventRepository;
import com.salonhub.onboarding.repo.SalonFeatureRepository;
import com.salonhub.onboarding.repo.SalonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
// A failed call to Meta is logged and then reported to the browser; without noRollbackFor the
// exception would roll back the log entry too.
@Transactional(noRollbackFor = MetaApiException.class)
public class OnboardingService {

    private final SalonRepository salons;
    private final SalonFeatureRepository features;
    private final OnboardingEventRepository events;
    private final MetaClient meta;
    private final MetaProperties metaProperties;
    private final Clock clock;

    public OnboardingService(SalonRepository salons, SalonFeatureRepository features,
                             OnboardingEventRepository events, MetaClient meta,
                             MetaProperties metaProperties, Clock clock) {
        this.salons = salons;
        this.features = features;
        this.events = events;
        this.meta = meta;
        this.metaProperties = metaProperties;
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
        Salon salon = salons.save(new Salon(trimmed, clock.instant()));
        log(salon, Type.SALON_CREATED, "Salon added");
        return salon;
    }

    public void deleteSalon(long id) {
        Salon salon = getSalon(id);
        events.deleteBySalonId(salon.getId());
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

    @Transactional(readOnly = true)
    public List<OnboardingEvent> eventsOf(long salonId) {
        return events.findBySalonIdOrderByOccurredAtDescIdDesc(salonId);
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
            log(salon, Type.FEATURE_ADDED, featureType.label() + " added: waiting for the salon to connect WhatsApp");
            updateStage(salon);
        } else {
            log(salon, Type.FEATURE_ADDED, featureType.label() + " added (demo only: live straight away)");
        }
    }

    // The salon clicked Connect WhatsApp and Meta's pop-up opened.
    public void signupLaunched(long salonId) {
        Salon salon = salonThatCanConnect(salonId);
        if (salon.isConnected()) {
            throw OnboardingException.conflict("This salon is already connected to WhatsApp.");
        }
        salon.setSignupState(SignupState.IN_PROGRESS);
        salon.setSignupAbandonedStep(null);
        salon.setSignupError(null);
        log(salon, Type.SIGNUP_LAUNCHED, "Meta signup pop-up opened");
        updateStage(salon);
    }

    public void signupSessionEvent(long salonId, SignupSessionEvent e) {
        Salon salon = salonThatCanConnect(salonId);
        if (e == null || e.event() == null || e.event().isBlank()) {
            throw OnboardingException.badRequest("Missing event.");
        }
        if (e.isFinish()) {
            salon.setSignupState(SignupState.FINISHED);
            salon.setSignupAbandonedStep(null);
            salon.setSignupError(null);
            if (notBlank(e.wabaId())) salon.setWabaId(e.wabaId());
            if (notBlank(e.phoneNumberId())) salon.setPhoneNumberId(e.phoneNumberId());
            if (notBlank(e.businessId())) salon.setBusinessId(e.businessId());
            log(salon, Type.SIGNUP_FINISHED, "Salon finished Meta signup");
            completeConnection(salon);
        } else if (salon.getSignupState() == SignupState.FINISHED) {
            // Closing the pop-up on its last screen also sends a cancel; the signup still counts as finished.
            return;
        } else if (e.isReportedError()) {
            String message = notBlank(e.errorMessage()) ? e.errorMessage() : "unspecified error";
            salon.setSignupState(SignupState.ERROR);
            salon.setSignupError(message + (notBlank(e.errorCode()) ? " (code " + e.errorCode() + ")" : ""));
            log(salon, Type.SIGNUP_ERROR, "Salon saw an error in Meta signup: " + salon.getSignupError());
        } else if (e.isCancel()) {
            salon.setSignupState(SignupState.ABANDONED);
            salon.setSignupAbandonedStep(e.currentStep());
            log(salon, Type.SIGNUP_ABANDONED, "Salon closed Meta signup" + NextActions.whereAbandoned(e.currentStep()));
        } else {
            throw OnboardingException.badRequest("Unknown event: " + e.event());
        }
        updateStage(salon);
    }

    // Meta's code expires 30 seconds after the pop-up closes, so it's exchanged straight away.
    public void signupCode(long salonId, String code) {
        Salon salon = salonThatCanConnect(salonId);
        if (code == null || code.isBlank()) {
            throw OnboardingException.badRequest("Missing code.");
        }
        // Meta only issues a code when the salon gets to the end of the pop-up.
        salon.setSignupState(SignupState.FINISHED);
        salon.setSignupAbandonedStep(null);
        salon.setSignupError(null);
        try {
            salon.setAccessToken(meta.exchangeCodeForToken(code.trim()));
        } catch (MetaApiException ex) {
            log(salon, Type.SIGNUP_ERROR, "Couldn't exchange Meta's code for the salon's token: " + ex.summary());
            updateStage(salon);
            throw ex;
        }
        completeConnection(salon);
        updateStage(salon);
    }

    // Runs once we have both the token and the WhatsApp account ID, whichever arrives last.
    private void completeConnection(Salon salon) {
        if (!salon.hasToken() || salon.getWabaId() == null) {
            return;
        }
        try {
            meta.subscribeApp(salon.getWabaId(), salon.getAccessToken());
            log(salon, Type.STATUS_CHANGED, "Connected to the salon's WhatsApp account");
            readStatus(salon);
        } catch (MetaApiException ex) {
            log(salon, Type.SIGNUP_ERROR, "Couldn't complete the connection: " + ex.summary());
        }
    }

    public void registerNumber(long salonId) {
        Salon salon = salonThatCanConnect(salonId);
        if (!salon.isConnected() || salon.getPhoneNumberId() == null) {
            throw OnboardingException.conflict("The salon needs to connect WhatsApp before its number can be registered.");
        }
        try {
            meta.registerPhoneNumber(salon.getPhoneNumberId(), metaProperties.getRegistrationPin(),
                    salon.getAccessToken());
        } catch (MetaApiException ex) {
            log(salon, Type.STATUS_CHANGED, "Couldn't register the phone number: " + ex.summary());
            throw ex;
        }
        salon.setNumberRegistered(true);
        log(salon, Type.NUMBER_REGISTERED, "Registered the salon's phone number for sending messages");
        readStatus(salon);
        updateStage(salon);
    }

    private void readStatus(Salon salon) {
        MetaStatus status = meta.fetchStatus(
                new MetaAssets(salon.getBusinessId(), salon.getWabaId(), salon.getPhoneNumberId()),
                salon.getAccessToken());
        if (!Objects.equals(status.phoneStatus(), salon.getPhoneStatus()) && status.phoneStatus() != null) {
            log(salon, Type.STATUS_CHANGED, "Phone number: " + orDash(salon.getPhoneStatus()) + " → " + status.phoneStatus());
            salon.setPhoneStatus(status.phoneStatus());
        }
        String verification = status.businessVerificationStatus();
        if (!Objects.equals(verification, salon.getBusinessVerificationStatus()) && verification != null) {
            log(salon, Type.STATUS_CHANGED,
                    "Business verification: " + orDash(salon.getBusinessVerificationStatus()) + " → " + verification);
            salon.setBusinessVerificationStatus(verification);
        }
    }

    private Salon salonThatCanConnect(long salonId) {
        Salon salon = getSalon(salonId);
        if (salon.isDemo()) {
            throw OnboardingException.badRequest(
                    "Demo salons can't connect to Meta. Add a new salon to try the real flow.");
        }
        if (!features.existsBySalonIdAndType(salon.getId(), FeatureType.WHATSAPP_REMINDERS)) {
            throw OnboardingException.badRequest("Add WhatsApp reminders to the salon first.");
        }
        return salon;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String orDash(String s) {
        return s == null ? "—" : s;
    }

    // Call after anything that could move the salon to another stage.
    void updateStage(Salon salon) {
        OnboardingStage next = StageCalculator.calculate(salon.stageInputs());
        OnboardingStage current = salon.getStage();
        if (next != current) {
            salon.setStage(next);
            salon.setStageSince(clock.instant());
            log(salon, Type.STAGE_CHANGED, current.label() + " → " + next.label());
        }
    }

    private void log(Salon salon, Type type, String detail) {
        events.save(new OnboardingEvent(salon.getId(), clock.instant(), type, detail));
    }
}
