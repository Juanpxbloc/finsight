package com.finsight.finsight_api.company;

import com.finsight.finsight_api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyResponse createCompany(CreateCompanyRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Company company = Company.builder()
                .name(request.getName())
                .sector(request.getSector())
                .taxId(request.getTaxId())
                .user(currentUser)
                .build();

        Company savedCompany = companyRepository.save(company);
        return mapToResponse(savedCompany);
    }

    public List<CompanyResponse> getUserCompanies() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return companyRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
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