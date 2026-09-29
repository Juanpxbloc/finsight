package com.finsight.finsight_api.company;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByUserId(Long userId);
    Optional<Company> findByIdAndUserId(Long id, Long userId);

    /** taxId is globally unique: no other company may repeat it. */
    boolean existsByTaxId(String taxId);

    /** Same as existsByTaxId, but ignoring the company currently being updated. */
    boolean existsByTaxIdAndIdNot(String taxId, Long id);
}
