package com.sosvideos.kiwify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the Kiwify webhook payload.
 * Field names follow Kiwify's API convention (mixed case).
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KiwifyWebhookPayload {

    @JsonProperty("webhook_event_type")
    private String webhookEventType;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("order_ref")
    private String orderRef;

    @JsonProperty("order_status")
    private String orderStatus;

    @JsonProperty("subscription_id")
    private String subscriptionId;

    @JsonProperty("Subscription")
    private SubscriptionData subscription;

    @JsonProperty("Customer")
    private CustomerData customer;

    // ── Nested: Subscription ──────────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubscriptionData {

        private String id;

        @JsonProperty("current_period")
        private CurrentPeriod currentPeriod;

        @Getter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CurrentPeriod {

            @JsonProperty("period_start_date")
            private String periodStartDate;

            @JsonProperty("period_end_date")
            private String periodEndDate;
        }
    }

    // ── Nested: Customer ──────────────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerData {

        private String id;

        @JsonProperty("full_name")
        private String fullName;

        private String email;

        @JsonProperty("CPF")
        private String cpf;
    }
}
