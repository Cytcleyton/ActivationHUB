package com.salonhub.onboarding.repo;

import com.salonhub.onboarding.domain.FeatureType;
import com.salonhub.onboarding.domain.SalonFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonFeatureRepository extends JpaRepository<SalonFeature, Long> {

    List<SalonFeature> findBySalonIdOrderByAddedAtAscIdAsc(Long salonId);

    List<SalonFeature> findAllByOrderByAddedAtAscIdAsc();

    boolean existsBySalonIdAndType(Long salonId, FeatureType type);

    void deleteBySalonId(Long salonId);
}
