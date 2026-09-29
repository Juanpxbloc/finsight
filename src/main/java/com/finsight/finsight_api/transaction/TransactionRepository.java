package com.finsight.finsight_api.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Obtener transacciones de una empresa asegurando que pertenezcan al usuario del token
    List<Transaction> findByCompanyIdAndCompanyUserId(Long companyId, Long userId);

    // Opcional: Eliminar transacciones previas antes de subir un nuevo CSV si deseas sobrescribir
    void deleteByCompanyIdAndCompanyUserId(Long companyId, Long userId);
}