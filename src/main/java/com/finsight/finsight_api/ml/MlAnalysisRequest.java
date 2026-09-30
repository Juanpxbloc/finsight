package com.finsight.finsight_api.ml;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload sent to the Python microservice. The wire format is snake_case,
 * which is what FastAPI/pydantic expects by default.
 */
@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MlAnalysisRequest {
    private Long companyId;
    private List<TransactionData> transactions;

    @Data
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class TransactionData {
        private BigDecimal amount;
        private String type;
        private String category;
        private String date;
    }
}
