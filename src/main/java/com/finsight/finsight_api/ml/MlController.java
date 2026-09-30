package com.finsight.finsight_api.ml;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class MlController {

    private final MlServiceClient mlServiceClient;

    @PostMapping("/analyze/{companyId}")
    public ResponseEntity<MlAnalysisResponse> runAnalysis(@PathVariable Long companyId) {
        MlAnalysisResponse response = mlServiceClient.analyzeCompanyData(companyId);
        return ResponseEntity.ok(response);
    }
}