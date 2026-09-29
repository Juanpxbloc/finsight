package com.finsight.finsight_api.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Constraints must mirror the Company entity: otherwise the failure surfaces on flush (500) instead of on validation (400). */
@Data
public class CreateCompanyRequest {
    @NotBlank
    @Size(min = 2, max = 150)
    private String name;
    @NotBlank
    @Size(min = 3, max = 100)
    private String sector;
    @Size(max = 50)
    private String taxId;
}