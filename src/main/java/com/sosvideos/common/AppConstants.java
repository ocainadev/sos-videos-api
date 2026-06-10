package com.sosvideos.common;

import java.time.LocalDate;
import java.time.ZoneId;

public final class AppConstants {

    private AppConstants() {}

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static final int VIDEO_VISIBILITY_DAYS = 7;
    public static final int FOLDER_EXPIRY_DAYS = VIDEO_VISIBILITY_DAYS;
    public static final int SUBSCRIPTION_EXPIRES = 30;

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
