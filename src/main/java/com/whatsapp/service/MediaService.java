package com.whatsapp.service;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.MediaDto;
import com.whatsapp.entity.ErrorCode;
import com.whatsapp.exception.SystemException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    @Transactional
    public MediaDto.MediaUploadResponse uploadMedia(Long userId, MultipartFile file, String type, Long conversationId, 
            boolean compress, boolean generateThumbnail) {
        
        if (file.isEmpty()) {
            throw new SystemException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        
        validateFile(file);
        
        String fileId = UUID.randomUUID().toString();
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String fileName = fileId + fileExtension;
        
        try {
            // Create assets directory if it doesn't exist
            Path assetsDir = Paths.get("src/main/resources/assets");
            Files.createDirectories(assetsDir);
            
            // Save file to assets directory
            Path filePath = assetsDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            
            String fileUrl = "/assets/" + fileName;
            
            log.info("File {} saved to {}", originalFileName, filePath.toString());
            
            return MediaDto.MediaUploadResponse.builder()
                    .fileId(fileId)
                    .fileName(originalFileName)
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .uploadedAt(LocalDateTime.now())
                    .build();
                    
        } catch (IOException e) {
            log.error("Failed to save file: {}", e.getMessage());
            throw new SystemException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public MediaDto.MediaResponse getMedia(Long userId, String mediaId) {
        // Mock implementation
        return MediaDto.MediaResponse.builder()
                .fileId(mediaId)
                .fileName("sample.jpg")
                .fileUrl("https://cdn.example.com/" + mediaId)
                .thumbnailUrl("https://cdn.example.com/thumbnails/" + mediaId)
                .fileSize(1024L)
                .mimeType("image/jpeg")
                .mediaType("IMAGE")
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public void deleteMedia(Long userId, String mediaId) {
        log.info("Deleting media {} for user {}", mediaId, userId);
        // Implementation for deleting media
    }

    @Transactional(readOnly = true)
    public MediaDto.MediaListResponse getConversationMedia(Long userId, Long conversationId, String type, int limit, int offset) {
        // Mock implementation
        List<MediaDto.MediaResponse> mediaList = new ArrayList<>();
        
        return MediaDto.MediaListResponse.builder()
                .media(mediaList)
                .pagination(ApiResponse.PaginationInfo.builder()
                        .page(offset / limit + 1)
                        .limit(limit)
                        .total(0)
                        .hasNext(false)
                        .build())
                .build();
    }

    private void validateFile(MultipartFile file) {
        // File size validation (10MB limit)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new SystemException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        
        // File type validation
        String contentType = file.getContentType();
        if (contentType == null || !isValidFileType(contentType)) {
            throw new SystemException(ErrorCode.FILE_INVALID_TYPE);
        }
    }

    private boolean isValidFileType(String contentType) {
        return contentType.startsWith("image/") || 
               contentType.startsWith("video/") || 
               contentType.startsWith("audio/") ||
               contentType.equals("application/pdf") ||
               contentType.startsWith("application/msword") ||
               contentType.startsWith("application/vnd.openxmlformats");
    }

    private String generateFileUrl(String fileId, String fileName) {
        return "/assets/" + fileId + getFileExtension(fileName);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}