package com.finsight.finsight_api.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final CsvUploadService csvUploadService;

    @PostMapping("/upload/{companyId}")
    public ResponseEntity<Map<String, Object>> uploadCsv(
            @PathVariable Long companyId,
            @RequestParam("file") MultipartFile file
    ) {
        int recordsProcessed = csvUploadService.processAndSaveCsv(companyId, file);
        return ResponseEntity.ok(Map.of(
                "message", " CSV processed correctly.",
                "recordsProcessed", recordsProcessed
        ));
    }
}