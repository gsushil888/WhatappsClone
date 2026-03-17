package com.whatsapp.service;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.MediaDto;
import com.whatsapp.entity.ErrorCode;
import com.whatsapp.exception.SystemException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${app.upload.dir}")
	private String uploadDir;

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
		// Save with original filename instead of UUID
		String fileName = sanitizeFileName(originalFileName);
		String uniqueFileName = fileId + "_" + fileName;

		try {
			Path uploadsDir = Paths.get(uploadDir);
			Files.createDirectories(uploadsDir);

			// Save file with unique name
			Path filePath = uploadsDir.resolve(uniqueFileName);
			Files.copy(file.getInputStream(), filePath);

			String fileUrl = "/assets/" + uniqueFileName;

			log.info("File {} saved to {}", originalFileName, filePath.toString());

			return MediaDto.MediaUploadResponse.builder().fileId(fileId).fileName(originalFileName).fileUrl(fileUrl)
					.fileSize(file.getSize()).mimeType(file.getContentType()).uploadedAt(LocalDateTime.now()).build();

		} catch (IOException e) {
			log.error("Failed to save file: {}", e.getMessage());
			throw new SystemException(ErrorCode.FILE_UPLOAD_FAILED);
		}
	}

	@Transactional(readOnly = true)
	public MediaDto.MediaResponse getMedia(Long userId, String mediaId) {
		try {
			Path uploadsDir = Paths.get(uploadDir);
			
			// Find file with matching fileId prefix
			Path[] matchingFiles = Files.list(uploadsDir)
					.filter(path -> path.getFileName().toString().startsWith(mediaId))
					.toArray(Path[]::new);
			
			if (matchingFiles.length == 0) {
				throw new SystemException(ErrorCode.FILE_NOT_FOUND);
			}
			
			Path filePath = matchingFiles[0];
			String fullFileName = filePath.getFileName().toString();
			String originalFileName = fullFileName.substring(fullFileName.indexOf("_") + 1);
			long fileSize = Files.size(filePath);
			String mimeType = Files.probeContentType(filePath);
			
			return MediaDto.MediaResponse.builder()
					.fileId(mediaId)
					.fileName(originalFileName)
					.fileUrl("/assets/" + fullFileName)
					.fileSize(fileSize)
					.mimeType(mimeType)
					.mediaType(getMediaType(mimeType))
					.uploadedBy(userId)
					.uploadedAt(LocalDateTime.now())
					.build();
					
		} catch (IOException e) {
			log.error("Failed to get media: {}", e.getMessage());
			throw new SystemException(ErrorCode.FILE_NOT_FOUND);
		}
	}

	@Transactional
	public void deleteMedia(Long userId, String mediaId) {
		log.info("Deleting media {} for user {}", mediaId, userId);
		// Implementation for deleting media
	}

	@Transactional(readOnly = true)
	public MediaDto.MediaListResponse getConversationMedia(Long userId, Long conversationId, String type, int limit,
			int offset) {
		// Mock implementation
		List<MediaDto.MediaResponse> mediaList = new ArrayList<>();

		return MediaDto.MediaListResponse.builder().media(mediaList).pagination(ApiResponse.PaginationInfo.builder()
				.page(offset / limit + 1).limit(limit).total(0).hasNext(false).build()).build();
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
		return contentType.startsWith("image/") || contentType.startsWith("video/") || contentType.startsWith("audio/")
				|| contentType.equals("application/pdf") || contentType.startsWith("application/msword")
				|| contentType.startsWith("application/vnd.openxmlformats");
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

	private String sanitizeFileName(String fileName) {
		if (fileName == null) return "file";
		// Remove path traversal attempts and dangerous characters
		return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
	}
	
	private String getMediaType(String mimeType) {
		if (mimeType == null) return "FILE";
		if (mimeType.startsWith("image/")) return "IMAGE";
		if (mimeType.startsWith("video/")) return "VIDEO";
		if (mimeType.startsWith("audio/")) return "AUDIO";
		return "DOCUMENT";
	}
}