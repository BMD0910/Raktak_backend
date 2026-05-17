package com.raktakk.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnitechPayClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${unitech.api.base-url}")
    private String baseUrl;

    @Value("${unitech.api.key:}")
    private String apiKey;

    public record PaymentResponse(boolean success, Map<String,Object> data, String message) {}

    private Map<String, Object> toStringObjectMap(Map<?, ?> source) {
        if (source == null) return null;
        Map<String, Object> target = new java.util.HashMap<>();
        source.forEach((k, v) -> {
            if (k != null) target.put(String.valueOf(k), v);
        });
        return target;
    }

    private PaymentResponse createPayment(String action, Long amount, String customerNumber, String description, String callbackSuccess, String callbackCancel) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("UNITECH_API_KEY manquante: impossible d'appeler {}", action);
            return new PaymentResponse(false, null, "UNITECH_API_KEY manquante. Configurez la clé UnitechPay avant de tester les paiements.");
        }

        String url = baseUrl + "?action=" + action;
        Map<String, Object> body = Map.of(
                "amount", amount,
                "customer_number", customerNumber == null ? "" : customerNumber,
                "description", description == null ? "" : description,
                "callback_success", callbackSuccess,
                "callback_cancel", callbackCancel
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) headers.setBearerAuth(apiKey);

        HttpEntity<Map<String,Object>> req = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Object> resp = restTemplate.postForEntity(url, req, Object.class);
            Map<String,Object> bodyResp = resp.getBody() instanceof Map<?,?> rawBody
                    ? toStringObjectMap(rawBody)
                    : null;
            boolean success = bodyResp != null && Boolean.TRUE.equals(bodyResp.get("success"));
            Map<String,Object> data = null;
            if (success && bodyResp.get("data") instanceof Map<?,?> rawData) {
                data = toStringObjectMap(rawData);
            }
            return new PaymentResponse(success, data, success ? null : String.valueOf(bodyResp != null ? bodyResp.get("message") : "Réponse invalide de UnitechPay"));
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("UnitechPay 401 sur {} (clé absente/invalide ?). BaseURL={}, keyConfigured={}", action, baseUrl, apiKey != null && !apiKey.isBlank());
            return new PaymentResponse(false, null, "UnitechPay a refusé la requête (401). Vérifiez UNITECH_API_KEY et la base URL.");
        } catch (Exception e) {
            log.error("Erreur appel UnitechPay {}: {}", action, e.getMessage(), e);
            return new PaymentResponse(false, null, e.getMessage());
        }
    }

    public PaymentResponse createWavePayment(Long amount, String customerNumber, String description, String callbackSuccess, String callbackCancel) {
        return createPayment("create_wave_payment", amount, customerNumber, description, callbackSuccess, callbackCancel);
    }

    public PaymentResponse createOrangeQRPayment(Long amount, String customerNumber, String description, String callbackSuccess, String callbackCancel) {
        return createPayment("create_orange_qr", amount, customerNumber, description, callbackSuccess, callbackCancel);
    }

    public PaymentResponse createOrangeMaxItPayment(Long amount, String customerNumber, String description, String callbackSuccess, String callbackCancel) {
        return createPayment("create_orange_maxit", amount, customerNumber, description, callbackSuccess, callbackCancel);
    }

    public PaymentResponse createOrangeOMPayment(Long amount, String customerNumber, String description, String callbackSuccess, String callbackCancel) {
        return createPayment("create_orange_om", amount, customerNumber, description, callbackSuccess, callbackCancel);
    }
}
