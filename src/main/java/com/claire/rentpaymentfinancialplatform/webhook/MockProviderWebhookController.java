package com.claire.rentpaymentfinancialplatform.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/provider-webhooks/mock-provider")
public class MockProviderWebhookController {

    private final MockProviderWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public MockProviderWebhookController(MockProviderWebhookService webhookService, ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ProviderWebhookResponse receive(
            @RequestHeader("X-Mock-Provider-Signature") String signature,
            @Valid @RequestBody JsonNode rawPayload
    ) {
        try {
            MockProviderWebhookRequest request = objectMapper.treeToValue(rawPayload, MockProviderWebhookRequest.class);
            return webhookService.receive(signature, request, objectMapper.writeValueAsString(rawPayload));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Webhook payload cannot be processed.", exception);
        }
    }
}
