package com.finsight.finsight_api.ml;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Response of the Python microservice, and also what this API returns to its
 * own clients.
 *
 * The microservice speaks snake_case (FastAPI/pydantic default), so every field
 * declares its snake_case name as an alias: without it a naming mismatch does
 * not fail, it silently leaves every field null. Aliases only affect reading,
 * so the response we serialize stays camelCase like the rest of the API.
 */
@Data
public class MlAnalysisResponse {

    /** Ej: 0.78 (Riesgo Alto) */
    @JsonAlias("risk_score")
    private Double riskScore;

    /** Ej: "HIGH", "MEDIUM", "LOW" */
    @JsonAlias("risk_level")
    private String riskLevel;

    /** Factores de mayor impacto (SHAP) */
    @JsonAlias("shap_values")
    private Map<String, Double> shapValues;

    /** Mensajes de alerta sugeridos */
    @JsonAlias("generated_alerts")
    private List<String> generatedAlerts;
}
