package com.salonhub.onboarding.web;

import com.salonhub.onboarding.service.OnboardingService;
import com.salonhub.onboarding.web.Dtos.Overview;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OverviewController {

    private final OnboardingService service;

    public OverviewController(OnboardingService service) {
        this.service = service;
    }

    @GetMapping("/api/overview")
    public Overview overview() {
        return SalonViews.overview(service.listSalons(), service.featuresBySalon());
    }
}
