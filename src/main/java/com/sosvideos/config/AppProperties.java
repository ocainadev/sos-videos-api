package com.sosvideos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static com.sosvideos.common.AppConstants.SUBSCRIPTION_EXPIRES;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Google google = new Google();
    private Admin admin = new Admin();
    private Kiwify kiwify = new Kiwify();
    private String corsAllowedOrigins = "http://localhost:5500,http://127.0.0.1:5500";
    private int subscriptionDurationDays = SUBSCRIPTION_EXPIRES;

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs;
    }

    @Data
    public static class Google {
        private Drive drive = new Drive();

        @Data
        public static class Drive {
            private String credentialsPath;
            private String applicationName;
            private String tokensDirectoryPath;
        }
    }

    @Data
    public static class Admin {
        private String email;
        private String password;
    }

    @Data
    public static class Kiwify {
        private String webhookSecret;
    }
}
