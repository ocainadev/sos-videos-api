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

    /**
     * Lists video files inside a Drive folder (paginated), capped at {@value MAX_LIST_FILES} files.
     * Hard cap prevents unbounded memory usage on very large folders.
     */
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

    /**
     * Builds a short-lived authenticated URL that the browser can use to download
     * the file DIRECTLY from Google Drive — bypassing the VPS entirely.
     *
     * <p>How it works: the service-account access token (scoped to DRIVE_READONLY,
     * valid ~1 hour, auto-refreshed) is embedded as a query parameter. The browser
     * navigates to this URL and downloads from Google's CDN at full speed.</p>
     *
     * <p>Security note: the token is transmitted over HTTPS only, is scoped to
     * read-only, and expires automatically. It is NOT stored anywhere by this service.</p>
     */
    public String buildTempDownloadUrl(String fileId) throws IOException {
        credentials.refreshIfExpired();
        String accessToken = credentials.getAccessToken().getTokenValue();
        String encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        return "https://www.googleapis.com/drive/v3/files/"
                + fileId + "?alt=media&access_token=" + encodedToken;
    }

    /**
     * Streams a Drive file content directly to the provided OutputStream.
     * The link is never exposed to the client.
     * Buffer de 256KB para melhor throughput no streaming.
     *
     * <p>Fallback path only — prefer {@link #buildTempDownloadUrl} for browser downloads.</p>
     */
    public void streamFile(String fileId, OutputStream outputStream) throws IOException {
        var buffered = new java.io.BufferedOutputStream(outputStream, 256 * 1024);
        drive.files().get(fileId).executeMediaAndDownloadTo(buffered);
        buffered.flush();
    }

    /**
     * Returns metadata of a single file.
     */
    public File getFileMetadata(String fileId) throws IOException {
        return drive.files().get(fileId)
                .setFields("id, name, mimeType, size")
                .execute();
    }

    /**
     * Exports a Google Workspace file (e.g. Google Doc auto-converted from .txt)
     * as plain text to the provided OutputStream.
     */
    public void exportAsPlainText(String fileId, OutputStream outputStream) throws IOException {
        drive.files().export(fileId, "text/plain")
                .executeMediaAndDownloadTo(outputStream);
    }

}
