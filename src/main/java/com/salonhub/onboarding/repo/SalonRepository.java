package com.salonhub.onboarding.repo;

import com.salonhub.onboarding.domain.Salon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    List<Salon> findAllByOrderByCreatedAtDescIdDesc();
}
