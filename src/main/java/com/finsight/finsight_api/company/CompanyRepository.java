package com.finsight.finsight_api.company;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByUserId(Long userId);
    Optional<Company> findByIdAndUserId(Long id, Long userId);

    /** El taxId es unico a nivel global: ninguna otra empresa puede repetirlo. */
    boolean existsByTaxId(String taxId);

    /** Igual que existsByTaxId, pero ignorando la empresa que se esta actualizando. */
    boolean existsByTaxIdAndIdNot(String taxId, Long id);
}
