package com.finsight.finsight_api.company;

import com.finsight.finsight_api.company.CompanyResponse;
import com.finsight.finsight_api.company.CreateCompanyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.createCompany(request));
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getMyCompanies() {
        return ResponseEntity.ok(companyService.getUserCompanies());
    }
}