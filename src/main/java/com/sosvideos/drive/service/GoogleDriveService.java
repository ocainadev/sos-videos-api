package com.sosvideos.drive.service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GoogleDriveService {

    private final Drive drive;
    private final GoogleCredentials credentials;

    public GoogleDriveService(@Lazy Drive drive, @Lazy GoogleCredentials credentials) {
        this.drive = drive;
        this.credentials = credentials;
    }

    private static final int MAX_LIST_FILES = 500;

    public List<File> listVideoFiles(String folderId) throws IOException {
        List<File> result = new ArrayList<>();
        String pageToken = null;

        do {
            FileList fileList = drive.files().list()
                    .setQ("'" + folderId + "' in parents and trashed = false")
                    .setFields("nextPageToken, files(id, name, mimeType, size, thumbnailLink)")
                    .setPageSize(100)
                    .setPageToken(pageToken)
                    .execute();

            if (fileList.getFiles() != null) {
                result.addAll(fileList.getFiles());
            }
            pageToken = fileList.getNextPageToken();

            if (result.size() >= MAX_LIST_FILES) {
                log.warn("listVideoFiles: folder {} has >= {} files — truncating result set", folderId, MAX_LIST_FILES);
                break;
            }
        } while (pageToken != null);

        log.debug("Listed {} files in folder {}", result.size(), folderId);
        return result;
    }

    public String buildTempDownloadUrl(String fileId) throws IOException {
        credentials.refreshIfExpired();
        String accessToken = credentials.getAccessToken().getTokenValue();
        String encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        return "https://www.googleapis.com/drive/v3/files/"
                + fileId + "?alt=media&access_token=" + encodedToken;
    }
    public void streamFile(String fileId, OutputStream outputStream) throws IOException {
        var buffered = new java.io.BufferedOutputStream(outputStream, 256 * 1024);
        drive.files().get(fileId).executeMediaAndDownloadTo(buffered);
        buffered.flush();
    }

    public File getFileMetadata(String fileId) throws IOException {
        return drive.files().get(fileId)
                .setFields("id, name, mimeType, size")
                .execute();
    }

    public void exportAsPlainText(String fileId, OutputStream outputStream) throws IOException {
        drive.files().export(fileId, "text/plain")
                .executeMediaAndDownloadTo(outputStream);
    }

}
