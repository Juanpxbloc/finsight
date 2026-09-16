package com.finsight.finsight_api.company;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyResponse {
    private Long id;
    private String name;
    private String sector;
    private String taxId;
    private Long userId;
}
