package com.whatsapp.controller;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.MediaDto;
import com.whatsapp.service.MediaService;
import com.whatsapp.util.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<MediaDto.MediaUploadResponse>> uploadMedia(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "conversationId", required = false) Long conversationId,
            @RequestParam(value = "compress", defaultValue = "true") boolean compress,
            @RequestParam(value = "generate_thumbnail", defaultValue = "true") boolean generateThumbnail) {

        Long userId = (Long) authentication.getPrincipal();
        MediaDto.MediaUploadResponse response = mediaService.uploadMedia(userId, file, type, conversationId, compress, generateThumbnail);

        return ResponseEntity.ok(ApiResponse.<MediaDto.MediaUploadResponse>builder()
                .success(true)
                .data(response)
                .correlationId(RequestContext.getCorrelationId())
                .message("Media uploaded successfully")
                .build());
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<MediaDto.MediaResponse>> getMedia(
            Authentication authentication,
            @PathVariable String mediaId) {

        Long userId = (Long) authentication.getPrincipal();
        MediaDto.MediaResponse response = mediaService.getMedia(userId, mediaId);

        return ResponseEntity.ok(ApiResponse.<MediaDto.MediaResponse>builder()
                .success(true)
                .data(response)
                .correlationId(RequestContext.getCorrelationId())
                .build());
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            Authentication authentication,
            @PathVariable String mediaId) {

        Long userId = (Long) authentication.getPrincipal();
        mediaService.deleteMedia(userId, mediaId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .correlationId(RequestContext.getCorrelationId())
                .message("Media deleted successfully")
                .build());
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ApiResponse<MediaDto.MediaListResponse>> getConversationMedia(
            Authentication authentication,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String type) {

        Long userId = (Long) authentication.getPrincipal();
        MediaDto.MediaListResponse response = mediaService.getConversationMedia(userId, conversationId, type, limit, offset);

        return ResponseEntity.ok(ApiResponse.<MediaDto.MediaListResponse>builder()
                .success(true)
                .data(response)
                .correlationId(RequestContext.getCorrelationId())
                .build());
    }
}