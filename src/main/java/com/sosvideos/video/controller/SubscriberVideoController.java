package com.sosvideos.video.controller;

import com.sosvideos.common.ApiResponse;
import com.sosvideos.security.UserPrincipal;
import com.sosvideos.video.dto.VideosByDateResponse;
import com.sosvideos.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscriber")
@PreAuthorize("hasAnyRole('SUBSCRIBER', 'ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Subscriber — Vídeos", description = "Listagem de vídeos do assinante")
@SecurityRequirement(name = "bearerAuth")
public class SubscriberVideoController {

    private final VideoService videoService;

    @GetMapping("/videos")
    @Operation(summary = "Lista vídeos visíveis ao assinante, agrupados por data")
    public ResponseEntity<ApiResponse<List<VideosByDateResponse>>> getVideos(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<VideosByDateResponse> response = videoService.getVisibleVideos(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
