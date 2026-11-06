package com.salonhub.onboarding.web;

import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.domain.SalonFeature;
import com.salonhub.onboarding.service.OnboardingService;
import com.salonhub.onboarding.web.Dtos.CreateSalonRequest;
import com.salonhub.onboarding.web.Dtos.SalonDetail;
import com.salonhub.onboarding.web.Dtos.SalonList;
import com.salonhub.onboarding.web.Dtos.SalonSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salons")
public class SalonController {

    private final OnboardingService service;

    public SalonController(OnboardingService service) {
        this.service = service;
    }

    @GetMapping
    public SalonList list() {
        Map<Long, List<SalonFeature>> features = service.featuresBySalon();
        return new SalonList(service.listSalons().stream()
                .map(s -> SalonViews.summary(s, features.getOrDefault(s.getId(), List.of())))
                .toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalonSummary create(@RequestBody CreateSalonRequest request) {
        return SalonViews.summary(service.createSalon(request.name()), List.of());
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

    private SalonDetail detail(long id) {
        Salon salon = service.getSalon(id);
        return SalonViews.detail(salon, service.featuresOf(id));
    }
}
