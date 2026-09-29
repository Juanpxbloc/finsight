package com.finsight.finsight_api.transaction;

import com.finsight.finsight_api.common.ApiException;
import com.finsight.finsight_api.company.Company;
import com.finsight.finsight_api.company.CompanyRepository;
import com.finsight.finsight_api.user.User;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CsvUploadService {

    private static final String COL_AMOUNT = "amount";
    private static final String COL_TYPE = "type";
    private static final String COL_CATEGORY = "category";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_DATE = "date";

    private static final List<String> REQUIRED_COLUMNS = List.of(COL_DATE, COL_TYPE, COL_CATEGORY, COL_AMOUNT);

    /** Mirrors the limits declared in the Transaction entity, so a bad row fails as 400 instead of 500 on flush. */
    private static final int CATEGORY_MIN = 3;
    private static final int CATEGORY_MAX = 100;
    private static final int DESCRIPTION_MAX = 500;

    private static final char BOM = '﻿';

    private final TransactionRepository transactionRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public int processAndSaveCsv(Long companyId, MultipartFile file) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Company company = companyRepository.findByIdAndUserIdAndActiveTrue(companyId, currentUser.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Company " + companyId + " does not exist or does not belong to the current user"));

        if (file == null || file.isEmpty()) {
            throw badRequest("CSV file is empty or not provided");
        }

        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader fileReader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(skipByteOrderMark(fileReader),
                     CSVFormat.DEFAULT.builder()
                             .setHeader()
                             .setSkipHeaderRecord(true)
                             .setIgnoreHeaderCase(true)
                             .setTrim(true)
                             .build())) {

            validateHeaders(csvParser);

            for (CSVRecord csvRecord : csvParser) {
                transactions.add(toTransaction(csvRecord, company));
            }

        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "We could not read the CSV file: " + e.getMessage(), e);
        }

        if (transactions.isEmpty()) {
            throw badRequest("No transactions found in the CSV file");
        }

        transactionRepository.saveAll(transactions);
        return transactions.size();
    }

    /** Excel exports UTF-8 with a byte-order mark, which would otherwise read as the header "﻿date". */
    private BufferedReader skipByteOrderMark(BufferedReader reader) throws IOException {
        reader.mark(1);
        if (reader.read() != BOM) {
            reader.reset();
        }
        return reader;
    }

    private void validateHeaders(CSVParser csvParser) {
        Set<String> headers = new LinkedHashSet<>();
        csvParser.getHeaderMap().keySet().forEach(header -> headers.add(header.trim().toLowerCase()));

        List<String> missing = REQUIRED_COLUMNS.stream()
                .filter(column -> !headers.contains(column))
                .toList();

        if (!missing.isEmpty()) {
            throw badRequest("Missing mandatory columns in the file header: " + String.join(", ", missing)
                    + ". Expected: " + String.join(", ", REQUIRED_COLUMNS)
                    + " (description is optional)");
        }
    }

    private Transaction toTransaction(CSVRecord csvRecord, Company company) {
        long row = csvRecord.getRecordNumber();

        return Transaction.builder()
                .company(company)
                .amount(parseAmount(required(csvRecord, COL_AMOUNT, row), row))
                .type(parseType(required(csvRecord, COL_TYPE, row), row))
                .category(parseCategory(required(csvRecord, COL_CATEGORY, row), row))
                .description(parseDescription(optional(csvRecord, COL_DESCRIPTION), row))
                .transactionDate(parseDate(required(csvRecord, COL_DATE, row), row))
                .build();
    }

    private String required(CSVRecord csvRecord, String column, long row) {
        String value = optional(csvRecord, column);
        if (value == null) {
            throw badRequest("Row " + row + ": missing value for column '" + column + "'");
        }
        return value;
    }

    private String optional(CSVRecord csvRecord, String column) {
        if (!csvRecord.isMapped(column) || !csvRecord.isSet(column)) {
            return null;
        }
        String value = csvRecord.get(column);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private BigDecimal parseAmount(String raw, long row) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw badRequest("Row " + row + ": amount '" + raw
                    + "' is not a valid number. Use a decimal point, for example 1500.50");
        }
        if (amount.signum() < 0) {
            throw badRequest("Row " + row + ": amount cannot be negative (" + raw
                    + "). Use the 'type' column to distinguish INCOME from EXPENSE");
        }
        return amount;
    }

    private TransactionType parseType(String raw, long row) {
        try {
            return TransactionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw badRequest("Row " + row + ": type '" + raw + "' is not valid. Allowed values: INCOME, EXPENSE");
        }
    }

    private String parseCategory(String raw, long row) {
        if (raw.length() < CATEGORY_MIN || raw.length() > CATEGORY_MAX) {
            throw badRequest("Row " + row + ": category must have between " + CATEGORY_MIN
                    + " and " + CATEGORY_MAX + " characters (received: '" + raw + "')");
        }
        return raw;
    }

    private String parseDescription(String raw, long row) {
        if (raw != null && raw.length() > DESCRIPTION_MAX) {
            throw badRequest("Row " + row + ": description exceeds " + DESCRIPTION_MAX + " characters");
        }
        return raw;
    }

    private LocalDate parseDate(String raw, long row) {
        LocalDate date;
        try {
            date = LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw badRequest("Row " + row + ": date '" + raw + "' is not valid. Expected format: YYYY-MM-DD");
        }
        if (date.isAfter(LocalDate.now())) {
            throw badRequest("Row " + row + ": date " + raw + " cannot be in the future. Use today or a past date.");
        }
        return date;
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
