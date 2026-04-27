package com.sosvideos.admin.dto;

import java.util.List;

public record CsvImportResult(
        int created,
        int updated,
        int skipped,
        List<String> errors
) {}
