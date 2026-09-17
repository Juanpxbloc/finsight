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

        if (taxId != null && companyRepository.existsByTaxId(taxId)) {
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

        if (taxId != null && companyRepository.existsByTaxIdAndIdNot(taxId, id)) {
            throw taxIdConflict(taxId);
        }

        company.setName(request.getName());
        company.setSector(request.getSector());
        company.setTaxId(taxId);

        return mapToResponse(companyRepository.save(company));
    }

    public void deleteCompany(Long id) {
        companyRepository.delete(findOwnedCompany(id));
    }

    public List<CompanyResponse> getUserCompanies() {
        return companyRepository.findByUserId(currentUser().getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Devuelve 404 tanto si la empresa no existe como si pertenece a otro usuario:
     * asi no se revela la existencia de empresas ajenas.
     */
    private Company findOwnedCompany(Long id) {
        return companyRepository.findByIdAndUserId(id, currentUser().getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "No existe una empresa con id " + id + " para este usuario"));
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** Cadena vacia o en blanco equivale a "sin taxId": evita chocar con la restriccion unica. */
    private String normalizeTaxId(String taxId) {
        if (taxId == null || taxId.isBlank()) {
            return null;
        }
        return taxId.trim();
    }

    private ApiException taxIdConflict(String taxId) {
        return new ApiException(HttpStatus.CONFLICT, "Ya existe una empresa registrada con el taxId: " + taxId);
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
