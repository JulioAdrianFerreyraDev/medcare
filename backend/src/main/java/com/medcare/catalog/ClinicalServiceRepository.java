package com.medcare.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalServiceRepository extends JpaRepository<ClinicalServiceEntity, UUID> {

    List<ClinicalServiceEntity> findAllByProviderId(String providerId);

    List<ClinicalServiceEntity> findAllByProviderIdAndStatus(String providerId, String status);
}
