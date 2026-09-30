package com.finsight.finsight_api.ml;

import com.finsight.finsight_api.common.ApiException;
import com.finsight.finsight_api.company.CompanyRepository;
import com.finsight.finsight_api.transaction.Transaction;
import com.finsight.finsight_api.transaction.TransactionRepository;
import com.finsight.finsight_api.user.User;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MlServiceClient {

    private final WebClient mlWebClient;
    private final TransactionRepository transactionRepository;
    private final CompanyRepository companyRepository;

    /** Backstop for block(): the connector timeouts should fire first. */
    @Value("${python.ml.service.response-timeout-ms:30000}")
    private long responseTimeoutMs;

    public MlAnalysisResponse analyzeCompanyData(Long companyId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Same ownership rule as the CSV upload: an unknown, foreign or
        // soft-deleted company is a 404, not an empty analysis.
        companyRepository.findByIdAndUserIdAndActiveTrue(companyId, currentUser.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Company " + companyId + " does not exist or does not belong to the current user"));

        List<Transaction> transactions = transactionRepository.findByCompanyIdAndCompanyUserId(companyId, currentUser.getId());

        if (transactions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "There are no transactions registered for analysis in this company.");
        }

        // Construir el payload para el modelo de Python
        List<MlAnalysisRequest.TransactionData> transactionDataList = transactions.stream()
                .map(t -> MlAnalysisRequest.TransactionData.builder()
                        .amount(t.getAmount())
                        .type(t.getType().name())
                        .category(t.getCategory())
                        .date(t.getTransactionDate().toString())
                        .build())
                .toList();

        MlAnalysisRequest requestPayload = MlAnalysisRequest.builder()
                .companyId(companyId)
                .transactions(transactionDataList)
                .build();

        // Llamada REST al microservicio de Python (/api/v1/predict)
        try {
            return mlWebClient.post()
                    .uri("/api/v1/predict")
                    .bodyValue(requestPayload)
                    .retrieve()
                    // A rejected shared secret is our misconfiguration, not bad input:
                    // log it loudly so it is not mistaken for a data problem.
                    .onStatus(status -> status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN,
                            response -> {
                                log.error("The ML microservice rejected our credentials ({}). "
                                        + "Check that python.ml.service.api-key matches the secret "
                                        + "configured in the microservice.", response.statusCode());
                                return response.releaseBody().then(Mono.just(new ApiException(
                                        HttpStatus.BAD_GATEWAY,
                                        "The analysis service is not correctly configured")));
                            })
                    .onStatus(HttpStatusCode::is4xxClientError, response -> response
                            .bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new ApiException(HttpStatus.BAD_GATEWAY,
                                    "The analysis service rejected the request")))
                    .onStatus(HttpStatusCode::is5xxServerError, response -> response
                            .bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new ApiException(HttpStatus.BAD_GATEWAY,
                                    "The analysis service failed to process the request")))
                    .bodyToMono(MlAnalysisResponse.class)
                    // Bloqueante sincrono para el flujo del controlador
                    .block(Duration.ofMillis(responseTimeoutMs + 1000));
        } catch (ApiException ex) {
            throw ex;
        } catch (WebClientRequestException ex) {
            if (ex.getCause() instanceof ReadTimeoutException) {
                throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                        "The analysis service took too long to respond", ex);
            }
            // Connection refused, DNS failure, connect timeout: the service is not reachable.
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The analysis service is not available", ex);
        } catch (WebClientResponseException ex) {
            // Safety net in case a status slips past the handlers above.
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "The analysis service returned an unexpected response", ex);
        } catch (IllegalStateException ex) {
            // block() timed out waiting for the response.
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                    "The analysis service took too long to respond", ex);
        }
    }
}
