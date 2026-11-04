package com.salonhub.onboarding.web;

import com.salonhub.onboarding.service.OnboardingService;
import com.salonhub.onboarding.web.Dtos.CreateSalonRequest;
import com.salonhub.onboarding.web.Dtos.SalonList;
import com.salonhub.onboarding.web.Dtos.SalonSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salons")
public class SalonController {

    private final OnboardingService service;

    public SalonController(OnboardingService service) {
        this.service = service;
    }

    @GetMapping
    public SalonList list() {
        return new SalonList(service.listSalons().stream().map(SalonSummary::of).toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalonSummary create(@RequestBody CreateSalonRequest request) {
        return SalonSummary.of(service.createSalon(request.name()));
    }
}
