package com.finsight.finsight_api.company;

import com.finsight.finsight_api.common.ApiException;
import com.finsight.finsight_api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyResponse createCompany(CreateCompanyRequest request) {
        User currentUser = currentUser();
        String taxId = normalizeTaxId(request.getTaxId());

        if (taxId != null && companyRepository.existsByTaxIdAndActiveTrue(taxId)) {
            throw taxIdConflict(taxId);
        }

        Company company = Company.builder()
                .name(request.getName())
                .sector(request.getSector())
                .taxId(taxId)
                .user(currentUser)
                .build();

        Company savedCompany = companyRepository.save(company);
        return mapToResponse(savedCompany);
    }

    public CompanyResponse updateCompany(Long id, CreateCompanyRequest request) {
        Company company = findOwnedCompany(id);
        String taxId = normalizeTaxId(request.getTaxId());

        if (taxId != null && companyRepository.existsByTaxIdAndActiveTrueAndIdNot(taxId, id)) {
            throw taxIdConflict(taxId);
        }

        company.setName(request.getName());
        company.setSector(request.getSector());
        company.setTaxId(taxId);

        return mapToResponse(companyRepository.save(company));
    }

    /**
     * Soft delete: the company is marked inactive and disappears from the API, but neither it
     * nor its transactions are erased, so the financial history stays auditable.
     */
    public void deleteCompany(Long id) {
        Company company = findOwnedCompany(id);
        company.setActive(false);
        companyRepository.save(company);
    }

    public List<CompanyResponse> getUserCompanies() {
        return companyRepository.findByUserIdAndActiveTrue(currentUser().getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Returns 404 when the company does not exist, belongs to another user, or was soft deleted,
     * so we never reveal the existence of someone else's company.
     */
    private Company findOwnedCompany(Long id) {
        return companyRepository.findByIdAndUserIdAndActiveTrue(id, currentUser().getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Company " + id + " does not exist or does not belong to the current user"));
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** An empty or blank string means "no taxId": avoids clashing with the unique index. */
    private String normalizeTaxId(String taxId) {
        if (taxId == null || taxId.isBlank()) {
            return null;
        }
        return taxId.trim();
    }

    private ApiException taxIdConflict(String taxId) {
        return new ApiException(HttpStatus.CONFLICT, "A company is already registered with taxId: " + taxId);
    }

    private CompanyResponse mapToResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .sector(company.getSector())
                .taxId(company.getTaxId())
                .userId(company.getUser().getId())
                .build();
    }
}
