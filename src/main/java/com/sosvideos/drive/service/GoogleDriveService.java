package com.sosvideos.drive.service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GoogleDriveService {

    private final Drive drive;

    public GoogleDriveService(@Lazy Drive drive) {
        this.drive = drive;
    }

    /**
     * Lists all video files inside a Drive folder (paginated).
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
        } while (pageToken != null);

        log.debug("Listed {} files in folder {}", result.size(), folderId);
        return result;
    }

    /**
     * Streams a Drive file content directly to the provided OutputStream.
     * The link is never exposed to the client.
     */
    public void streamFile(String fileId, OutputStream outputStream) throws IOException {
        drive.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream);
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
     * Streams the thumbnail of a Drive file to the provided OutputStream.
     * Always fetches a fresh thumbnailLink from the Drive API (links expire).
     * Uses plain HTTP (no auth header) because lh3.googleusercontent.com URLs
     * are pre-signed and reject OAuth tokens from a different domain.
     * Throws IOException if no thumbnail is available.
     */
}
