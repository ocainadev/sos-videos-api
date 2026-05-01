package com.sosvideos.common;

import java.time.LocalDate;
import java.time.ZoneId;

public final class AppConstants {

    private AppConstants() {}

    /** Timezone oficial do negócio — sempre São Paulo, independente do servidor. */
    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    /** Retorna a data atual no timezone de São Paulo. Use em vez de {@code LocalDate.now()}. */
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static final int VIDEO_VISIBILITY_DAYS = 7;

    /**
     * Folders whose publishDate is strictly older than (today - FOLDER_EXPIRY_DAYS) are
     * automatically purged. Must match VIDEO_VISIBILITY_DAYS so folders expire exactly when
     * their videos stop being visible to subscribers.
     */
    public static final int FOLDER_EXPIRY_DAYS = VIDEO_VISIBILITY_DAYS;

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUBSCRIBER = "ROLE_SUBSCRIBER";

    public static final String KIWIFY_PROVIDER = "KIWIFY";

    public static final String EVENT_ORDER_APPROVED = "order_approved";
    public static final String EVENT_SUBSCRIPTION_ACTIVE = "subscription_active";
    public static final String EVENT_SUBSCRIPTION_RENEWED = "subscription_renewed";
    public static final String EVENT_SUBSCRIPTION_CANCELED = "subscription_canceled";
    public static final String EVENT_SUBSCRIPTION_OVERDUE = "subscription_overdue";
    public static final String EVENT_ORDER_REFUNDED = "order_refunded";
}
