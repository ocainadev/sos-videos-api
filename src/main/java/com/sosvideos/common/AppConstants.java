package com.sosvideos.common;

public final class AppConstants {

    private AppConstants() {}

    public static final int VIDEO_VISIBILITY_DAYS = 7;

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
