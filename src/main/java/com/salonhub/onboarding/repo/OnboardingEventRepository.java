package com.salonhub.onboarding.repo;

import com.salonhub.onboarding.domain.OnboardingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingEventRepository extends JpaRepository<OnboardingEvent, Long> {

    List<OnboardingEvent> findBySalonIdOrderByOccurredAtDescIdDesc(Long salonId);

    void deleteBySalonId(Long salonId);
}
