package com.salonhub.onboarding.web;

import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.domain.SalonFeature;
import com.salonhub.onboarding.meta.GraphApiMetaClient;
import com.salonhub.onboarding.service.OnboardingService;
import com.salonhub.onboarding.service.SignupSessionEvent;
import com.salonhub.onboarding.web.Dtos.CreateSalonRequest;
import com.salonhub.onboarding.web.Dtos.SalonDetail;
import com.salonhub.onboarding.web.Dtos.SalonList;
import com.salonhub.onboarding.web.Dtos.SalonSummary;
import com.salonhub.onboarding.web.Dtos.SignupCodeRequest;
import com.salonhub.onboarding.web.Dtos.SimulateVerificationRequest;
import com.salonhub.onboarding.web.Dtos.SkipAheadRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salons")
public class SalonController {

    private final OnboardingService service;
    private final SalonViews views;

    public SalonController(OnboardingService service, SalonViews views) {
        this.service = service;
        this.views = views;
    }

    @GetMapping
    public SalonList list() {
        Map<Long, List<SalonFeature>> features = service.featuresBySalon();
        return new SalonList(service.listSalons().stream()
                .map(s -> views.summary(s, features.getOrDefault(s.getId(), List.of())))
                .toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalonSummary create(@RequestBody CreateSalonRequest request) {
        return views.summary(service.createSalon(request.name()), List.of());
    }

    @GetMapping("/{id}")
    public SalonDetail get(@PathVariable long id) {
        return detail(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.deleteSalon(id);
    }

    @PostMapping("/{id}/features/{type}")
    public SalonDetail addFeature(@PathVariable long id, @PathVariable String type) {
        service.addFeature(id, type);
        return detail(id);
    }

    // Called by the salon owner's view as Meta's signup pop-up opens and reports back.
    @PostMapping("/{id}/signup/launched")
    public SalonDetail signupLaunched(@PathVariable long id) {
        service.signupLaunched(id);
        return detail(id);
    }

    @PostMapping("/{id}/signup/session")
    public SalonDetail signupSession(@PathVariable long id, @RequestBody SignupSessionEvent event) {
        service.signupSessionEvent(id, event);
        return detail(id);
    }

    @PostMapping("/{id}/signup/code")
    public SalonDetail signupCode(@PathVariable long id, @RequestBody SignupCodeRequest request) {
        List<String> redirectUris = new ArrayList<>();
        if (request.dialogRedirectUris() != null) {
            redirectUris.addAll(request.dialogRedirectUris());
        }
        if (request.redirectUri() != null) {
            redirectUris.add(request.redirectUri());
            String root = GraphApiMetaClient.siteRoot(request.redirectUri());
            if (root != null) {
                redirectUris.add(root);
            }
        }
        service.signupCode(id, request.code(), redirectUris);
        return detail(id);
    }

    @PostMapping("/{id}/refresh")
    public SalonDetail refresh(@PathVariable long id) {
        service.refresh(id);
        return detail(id);
    }

    // Opened as a link from the salon's view: records it, then sends the browser on to Meta.
    @GetMapping("/{id}/verify-on-meta")
    public ResponseEntity<Void> verifyOnMeta(@PathVariable long id) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(service.openVerification(id))).build();
    }

    @PostMapping("/{id}/demo/verification")
    public SalonDetail simulateVerification(@PathVariable long id, @RequestBody SimulateVerificationRequest request) {
        service.simulateVerification(id, request.status());
        return detail(id);
    }

    @PostMapping("/{id}/demo/skip-ahead")
    public SalonDetail skipAhead(@PathVariable long id, @RequestBody SkipAheadRequest request) {
        service.skipAhead(id, request.days());
        return detail(id);
    }

    @PostMapping("/{id}/demo/reset")
    public SalonDetail resetConnection(@PathVariable long id) {
        service.resetConnection(id);
        return detail(id);
    }

    @PostMapping("/{id}/register-number")
    public SalonDetail registerNumber(@PathVariable long id) {
        service.registerNumber(id);
        return detail(id);
    }

    private SalonDetail detail(long id) {
        Salon salon = service.getSalon(id);
        return views.detail(salon, service.featuresOf(id), service.eventsOf(id));
    }
}
