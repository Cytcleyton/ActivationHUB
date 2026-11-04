package com.salonhub.onboarding.service;

import com.salonhub.onboarding.domain.Salon;
import com.salonhub.onboarding.repo.SalonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class OnboardingService {

    private final SalonRepository salons;

    public OnboardingService(SalonRepository salons) {
        this.salons = salons;
    }

    @Transactional(readOnly = true)
    public List<Salon> listSalons() {
        return salons.findAllByOrderByCreatedAtDescIdDesc();
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
        return salons.save(new Salon(trimmed, Instant.now()));
    }
}
