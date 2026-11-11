package com.salonhub.onboarding.web;

import com.salonhub.onboarding.service.OnboardingService;
import com.salonhub.onboarding.web.Dtos.Overview;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OverviewController {

    private final OnboardingService service;
    private final SalonViews views;

    public OverviewController(OnboardingService service, SalonViews views) {
        this.service = service;
        this.views = views;
    }

    @GetMapping("/api/overview")
    public Overview overview() {
        return views.overview(service.listSalons(), service.featuresBySalon());
    }
}
