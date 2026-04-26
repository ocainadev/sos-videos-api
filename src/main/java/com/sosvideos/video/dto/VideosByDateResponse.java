package com.sosvideos.video.dto;

import java.time.LocalDate;
import java.util.List;

public record VideosByDateResponse(
        LocalDate date,
        int totalVideos,
        List<VideoResponse> videos
) {}
