package com.finsight.finsight_api.company;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    // Soft-deleted companies (active = false) are invisible to the API
    List<Company> findByUserIdAndActiveTrue(Long userId);
    Optional<Company> findByIdAndUserIdAndActiveTrue(Long id, Long userId);

    /** taxId is unique among active companies: a soft-deleted company releases it for reuse. */
    boolean existsByTaxIdAndActiveTrue(String taxId);

    /** Same as existsByTaxIdAndActiveTrue, but ignoring the company currently being updated. */
    boolean existsByTaxIdAndActiveTrueAndIdNot(String taxId, Long id);
}
