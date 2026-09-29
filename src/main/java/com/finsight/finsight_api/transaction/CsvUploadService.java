package com.finsight.finsight_api.transaction;

import com.finsight.finsight_api.company.Company;
import com.finsight.finsight_api.company.CompanyRepository;
import com.finsight.finsight_api.user.User;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvUploadService {

    private final TransactionRepository transactionRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public int processAndSaveCsv(Long companyId, MultipartFile file) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Company company = companyRepository.findByIdAndUserId(companyId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found or access denied."));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded CSV file is empty.");
        }

        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build())) {

            for (CSVRecord csvRecord : csvParser) {
                Transaction transaction = Transaction.builder()
                        .company(company)
                        // 1. Mapeo de monto
                        .amount(new BigDecimal(csvRecord.get("monto")))

                        // 2. Mapeo de tipo (INGRESO / GASTO)
                        .type(TransactionType.valueOf(csvRecord.get("tipo").trim().toUpperCase()))

                        // 3. Mapeo de categoría
                        .category(csvRecord.get("categoria"))

                        // 4. Mapeo de descripción (opcional si viene vacía)
                        .description(csvRecord.isSet("descripcion") ? csvRecord.get("descripcion") : null)

                        // 5. Mapeo de fecha
                        .transactionDate(LocalDate.parse(csvRecord.get("fecha")))
                        .build();

                transactions.add(transaction);
            }
            // Inserción masiva en PostgreSQL
            transactionRepository.saveAll(transactions);
            return transactions.size();

        } catch (Exception e) {
            throw new RuntimeException("Error processing CSV: " + e.getMessage(), e);
        }
    }
}