package com.finsight.finsight_api.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Transactions of a company, ensuring they belong to the user in the token
    List<Transaction> findByCompanyIdAndCompanyUserId(Long companyId, Long userId);

    // Optional: delete previous transactions before uploading a new CSV, if you want to overwrite
    void deleteByCompanyIdAndCompanyUserId(Long companyId, Long userId);
}
