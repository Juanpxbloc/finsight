package com.finsight.finsight_api.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Las restricciones deben coincidir con las de la entidad Company: si no, el fallo salta en el flush (500) en vez de en la validacion (400). */
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