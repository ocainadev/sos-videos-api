package com.sosvideos.drive.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.sosvideos.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GoogleDriveConfig {

    private final AppProperties appProperties;

    @Bean
    @Lazy
    public GoogleCredentials googleCredentials() throws Exception {
        String credPath = appProperties.getGoogle().getDrive().getCredentialsPath();
        log.info("Loading Google credentials from: {}", credPath);
        try (InputStream credStream = openCredentials(credPath)) {
            return GoogleCredentials
                    .fromStream(credStream)
                    .createScoped(List.of(DriveScopes.DRIVE_READONLY));
        }
    }

    @Bean
    @Lazy
    public Drive googleDrive(GoogleCredentials googleCredentials) throws Exception {
        String appName = appProperties.getGoogle().getDrive().getApplicationName();

        HttpCredentialsAdapter credentialsAdapter = new HttpCredentialsAdapter(googleCredentials);
        HttpRequestInitializer initializer = request -> {
            credentialsAdapter.initialize(request);
            request.setConnectTimeout(15_000);
            request.setReadTimeout(300_000); // 5 min — safe for large-file streaming
        };

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                initializer)
                .setApplicationName(appName)
                .build();
    }

    private InputStream openCredentials(String path) throws IOException {
        ClassPathResource classpath = new ClassPathResource(path);
        if (classpath.exists()) {
            return classpath.getInputStream();
        }
        File file = new File(path);
        if (file.exists()) {
            return new FileInputStream(file);
        }
        throw new IOException("Google Drive credentials file not found: " + path
                + ". Set app.google.drive.credentials-path in application.properties.");
    }
}
