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
     * Streams a Drive file content directly to the provided OutputStream.
     * The link is never exposed to the client.
     * Buffer de 256KB para melhor throughput no streaming.
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
