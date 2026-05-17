package com.raktakk.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raktakk.backend.entity.SubscriptionTransaction;
import com.raktakk.backend.repository.SubscriptionTransactionRepository;
import com.raktakk.backend.service.VendorManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/public/unitech")
@RequiredArgsConstructor
@Slf4j
public class UnitechWebhookController {

    private final SubscriptionTransactionRepository transactionRepository;
    private final VendorManagementService vendorManagementService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${unitech.api.key:}")
    private String apiKey;

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestHeader Map<String,String> headers,
                                           @RequestBody String rawBody) {
        log.info("Received Unitech webhook raw: {}", rawBody);

        try {
            Map<String,Object> payload = objectMapper.readValue(rawBody, Map.class);

            // signature can be provided in header or inside payload
            String sigHeader = headers.getOrDefault("x-signature", headers.getOrDefault("signature", null));
            String sigPayload = payload.get("signature") == null ? null : String.valueOf(payload.get("signature"));
            String providedSig = sigHeader != null ? sigHeader : sigPayload;

            if (apiKey != null && !apiKey.isBlank() && providedSig != null) {
                if (!verifyHmacSha256(rawBody, apiKey, providedSig)) {
                    log.warn("Webhook signature invalid");
                    return ResponseEntity.status(401).body(Map.of("ok", false, "error", "invalid_signature"));
                }
            }

            String event = String.valueOf(payload.get("event"));
            String reference = String.valueOf(payload.get("reference"));

            if ("payment_completed".equals(event)) {
                transactionRepository.findByReference(reference).ifPresent(tx -> {
                    tx.setStatus("PAID");
                    transactionRepository.save(tx);
                    vendorManagementService.activateSubscriptionFromTransaction(tx.getId());
                });
            } else if ("payment_failed".equals(event)) {
                transactionRepository.findByReference(reference).ifPresent(tx -> {
                    tx.setStatus("FAILED");
                    transactionRepository.save(tx);
                });
            }

            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            log.error("Erreur traitement webhook Unitech: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/payment-success")
    public ResponseEntity<?> paymentSuccess(@RequestParam(name = "ref", required = false) String ref) {
        // La confirmation réelle doit venir via webhook; afficher page simple
        return ResponseEntity.ok(Map.of("ok", true, "message", "Paiement reçu. Nous confirmons la transaction bientôt.", "ref", ref));
    }

    @GetMapping("/payment-cancel")
    public ResponseEntity<?> paymentCancel(@RequestParam(name = "ref", required = false) String ref) {
        return ResponseEntity.ok(Map.of("ok", false, "message", "Paiement annulé.", "ref", ref));
    }

    private boolean verifyHmacSha256(String payload, String secret, String providedSig) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] macData = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String hex = bytesToHex(macData);
            String b64 = Base64.getEncoder().encodeToString(macData);
            return providedSig.equalsIgnoreCase(hex) || providedSig.equals(b64);
        } catch (Exception e) {
            log.error("Erreur vérification HMAC: {}", e.getMessage(), e);
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
