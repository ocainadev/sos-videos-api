package com.sosvideos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Google google = new Google();
    private Admin admin = new Admin();
    private Kiwify kiwify = new Kiwify();
    private String corsAllowedOrigins = "http://localhost:5500,http://127.0.0.1:5500";
    private int subscriptionDurationDays = 30;

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
    }

    @Getter
    @Setter
    public static class Google {
        private Drive drive = new Drive();

        @Getter
        @Setter
        public static class Drive {
            private String credentialsPath;
            private String applicationName;
            private String tokensDirectoryPath;
        }
    }

    @Getter
    @Setter
    public static class Admin {
        private String email;
        private String password;
    }

    @Getter
    @Setter
    public static class Kiwify {
        private String webhookSecret;
    }
}
