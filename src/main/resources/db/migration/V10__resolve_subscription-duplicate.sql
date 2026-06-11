CREATE UNIQUE INDEX uk_subscription_kiwify_id
    ON subscriptions(kiwify_subscription_id)
    WHERE kiwify_subscription_id IS NOT NULL;