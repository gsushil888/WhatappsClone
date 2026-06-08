package com.whatsapp.service;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.StoryDto;
import com.whatsapp.entity.*;
import com.whatsapp.exception.StoryException;
import com.whatsapp.exception.UserException;
import com.whatsapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

	private final StoryRepository storyRepository;
	private final UserRepository userRepository;
	private final ContactRepository contactRepository;
	private final StoryViewRepository storyViewRepository;
	private final SimpMessagingTemplate messagingTemplate;

	@Transactional(readOnly = true)
	public StoryDto.StoryFeedResponse getStoryFeed(Long userId, int limit, int offset) {
		Pageable pageable = PageRequest.of(offset / limit, limit);
		List<Story> stories = storyRepository.findStoryFeed(userId, pageable);

		List<StoryDto.StoryResponse> storyResponses = stories.stream()
				.map(story -> mapToStoryResponse(story, userId))
				.collect(Collectors.toList());

		return StoryDto.StoryFeedResponse.builder().stories(storyResponses)
				.pagination(ApiResponse.PaginationInfo.builder().page(offset / limit + 1).limit(limit)
						.total(storyResponses.size()).hasNext(storyResponses.size() == limit).build())
				.build();
	}

	@Transactional
	public StoryDto.StoryResponse postStory(Long userId, StoryDto.PostStoryRequest request) {
		User user = userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		Story.StoryType type = Story.StoryType.valueOf(request.getStoryType().toUpperCase());
		validateStoryRequest(type, request);

		Story story = Story.builder()
				.user(user)
				.type(type)
				.content(request.getContent())
				.mediaUrl(request.getMediaUrl())
				.thumbnailUrl(request.getThumbnailUrl())
				.backgroundColor(request.getBackgroundColor())
				.textStyle(request.getTextStyle())
				.linkUrl(request.getLinkUrl())
				.linkTitle(request.getLinkTitle())
				.linkDescription(request.getLinkDescription())
				.linkPreviewImage(request.getLinkPreviewImage())
				.privacy(Story.StoryPrivacy.valueOf(request.getPrivacySetting().toUpperCase()))
				.expiresAt(LocalDateTime.now().plusHours(24))
				.viewCount(0).isActive(true).build();

		story = storyRepository.save(story);
		return mapToStoryResponse(story, userId);
	}

	@Transactional
	public void viewStory(Long userId, Long storyId) {
		Story story = storyRepository.findById(storyId).orElseThrow(() -> new UserException(ErrorCode.STORY_NOT_FOUND));

		if (story.isExpired()) {
			throw new UserException(ErrorCode.STORY_EXPIRED);
		}
		
		// Only count unique views per user
		if (!storyViewRepository.existsByStoryIdAndUserId(storyId, userId)) {
			User viewer = userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
			storyViewRepository.save(StoryView.builder().story(story).user(viewer).build());

//START ADDED BY SUSHIL
		    // Owner viewing own story -> ignore
		    if (!story.getUser().getId().equals(userId)) {
		    	story.setViewCount(story.getViewCount() + 1);
		    }
//END 
			storyRepository.save(story);

			// Push real-time view count update to the story owner
			Map<String, Object> payload = new HashMap<>();
			payload.put("storyId", storyId);
			payload.put("viewCount", story.getViewCount());
			payload.put("viewerId", userId);
			payload.put("viewerName", viewer.getDisplayName());
			payload.put("viewerAvatar", viewer.getProfilePictureUrl());
			payload.put("viewedAt", LocalDateTime.now().toString());
			messagingTemplate.convertAndSendToUser(
					story.getUser().getId().toString(),
					"/queue/story-views",
					payload);

			log.info("Story {} viewed by user {} — total views: {}", storyId, userId, story.getViewCount());
		}
	}

	@Transactional(readOnly = true)
	public StoryDto.StoryViewersResponse getStoryViewers(Long userId, Long storyId) {
		Story story = storyRepository.findByIdAndUserId(storyId, userId)
				.orElseThrow(() -> new UserException(ErrorCode.STORY_NOT_FOUND));

		List<StoryView> views = storyViewRepository.findByStoryId(storyId);

//COMMENTED BY SUSHIL
//		List<StoryDto.StoryViewerResponse> viewers = views.stream()
//				.filter(v -> v.getUser().getId()!= userId)
//				.map(v -> StoryDto.StoryViewerResponse.builder()
//						.viewerId(v.getUser().getId())
//						.viewerName(v.getUser().getDisplayName())
//						.viewerAvatar(v.getUser().getProfilePictureUrl())
//						.viewedAt(v.getViewedAt())
//						.build())
//				.collect(Collectors.toList());
	
//ADDED ->UPDATED BY SUSHIL		
		List<StoryDto.StoryViewerResponse> viewers = views.stream()
			    .filter(v -> !v.getUser().getId().equals(userId))
			    .map(v -> {

			        String displayName = contactRepository
			            .findByViewerIdAndStoryOwnerId(
			            	story.getUser().getId(),
			                v.getUser().getId()
			                )
			            .map(c -> c.getDisplayName() != null
			                    ? c.getDisplayName()
			                    : v.getUser().getPhoneNumber())
			            .orElse(v.getUser().getPhoneNumber());

			        return StoryDto.StoryViewerResponse.builder()
			            .viewerId(v.getUser().getId())
			            .viewerName(displayName)
			            .viewerAvatar(v.getUser().getProfilePictureUrl())
			            .viewedAt(v.getViewedAt())
			            .build();
			    })
			    .toList();
// END 
		
		return StoryDto.StoryViewersResponse.builder()
				.viewers(viewers)
				.totalViewers(story.getViewCount())
				.build();
	}

	@Transactional
	public void deleteStory(Long userId, Long storyId) {
		Story story = storyRepository.findByIdAndUserId(storyId, userId)
				.orElseThrow(() -> new StoryException(ErrorCode.STORY_NOT_FOUND));

		story.setIsActive(false);
		storyRepository.save(story);

		log.info("Story {} deleted by user {}", storyId, userId);
	}

	@Transactional(readOnly = true)
	public StoryDto.StoryFeedResponse getMyStories(Long userId, int limit, int offset) {
		Pageable pageable = PageRequest.of(offset / limit, limit);
		List<Story> stories = storyRepository.findUserStories(userId, pageable);

		List<StoryDto.StoryResponse> storyResponses = stories.stream()
				.filter(story -> !story.isExpired())
				.map(story -> mapToStoryResponse(story, userId))
				.collect(Collectors.toList());

		return StoryDto.StoryFeedResponse.builder().stories(storyResponses)
				.pagination(ApiResponse.PaginationInfo.builder().page(offset / limit + 1).limit(limit)
						.total(storyResponses.size()).hasNext(storyResponses.size() == limit).build())
				.build();
	}

	@Transactional(readOnly = true)
	public StoryDto.UserStoriesResponse getUserStories(Long viewerId, Long userId, int limit, int offset) {
		User user = userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		Pageable pageable = PageRequest.of(offset / limit, limit);
		List<Story> stories = storyRepository.findUserStories(userId, pageable);

		List<StoryDto.StoryResponse> storyResponses = stories.stream()
				.filter(story -> !story.isExpired() && canViewStory(viewerId, story))
				.map(story -> mapToStoryResponse(story, viewerId))
				.collect(Collectors.toList());

		long unviewedCount = storyResponses.stream().filter(s -> Boolean.FALSE.equals(s.getIsViewed())).count();

		return StoryDto.UserStoriesResponse.builder().userId(userId).userName(user.getDisplayName())
				.userAvatar(user.getProfilePictureUrl()).stories(storyResponses)
				.unviewedCount((int) unviewedCount)
				.lastStoryAt(stories.isEmpty() ? null : stories.get(0).getCreatedAt()).build();
	}

	@Transactional
	public void cleanupExpiredStories() {
		List<Long> expiredIds = storyRepository.findExpiredStoryIds();
		if (expiredIds.isEmpty()) {
			return;
		}
		storyViewRepository.deleteByStoryIdIn(expiredIds);
		int deletedCount = storyRepository.deleteExpiredStories();
		log.info("Cleaned up {} expired stories", deletedCount);
	}

	private void validateStoryRequest(Story.StoryType type, StoryDto.PostStoryRequest request) {
		switch (type) {
			case TEXT -> {
				if (request.getContent() == null || request.getContent().isBlank())
					throw new StoryException(ErrorCode.STORY_CREATION_FAILED, "Content is required for TEXT story");
			}
			case IMAGE -> {
				if (request.getMediaUrl() == null || request.getMediaUrl().isBlank())
					throw new StoryException(ErrorCode.STORY_CREATION_FAILED, "mediaUrl is required for IMAGE story");
			}
			case VIDEO -> {
				if (request.getMediaUrl() == null || request.getMediaUrl().isBlank())
					throw new StoryException(ErrorCode.STORY_CREATION_FAILED, "mediaUrl is required for VIDEO story");
			}
			case LINK -> {
				if (request.getLinkUrl() == null || request.getLinkUrl().isBlank())
					throw new StoryException(ErrorCode.STORY_CREATION_FAILED, "linkUrl is required for LINK story");
			}
		}
	}

	private boolean canViewStory(Long viewerId, Story story) {
		if (story.getUser().getId().equals(viewerId)) {
			return true;
		}
		switch (story.getPrivacy()) {
		case PUBLIC:
			return true;
		case CONTACTS:
			return contactRepository.existsByUserIdAndContactUserId(story.getUser().getId(), viewerId);
		case CLOSE_FRIENDS:
		case CUSTOM:
			return false;
		default:
			return false;
		}
	}

	private StoryDto.StoryResponse mapToStoryResponse(Story story, Long currentUserId) {
		boolean isViewed = storyViewRepository.existsByStoryIdAndUserId(story.getId(), currentUserId);

		String displayName;
		if (story.getUser().getId().equals(currentUserId)) {
			displayName = "You";
		} else {
			displayName = contactRepository
					.findByViewerIdAndStoryOwnerId(currentUserId, story.getUser().getId())
					.map(c -> c.getDisplayName() != null ? c.getDisplayName() : story.getUser().getPhoneNumber())
					.orElse(story.getUser().getPhoneNumber());
		}

		boolean isOwner = story.getUser().getId().equals(currentUserId);

		return StoryDto.StoryResponse.builder()
				.id(story.getId())
				.userId(story.getUser().getId())
				.userName(displayName)
				.userAvatar(story.getUser().getProfilePictureUrl())
				.content(story.getContent())
				.mediaUrl(story.getMediaUrl())
				.thumbnailUrl(story.getThumbnailUrl())
				.storyType(story.getType().name())
				.backgroundColor(story.getBackgroundColor())
				.textStyle(story.getTextStyle())
				.linkUrl(story.getLinkUrl())
				.linkTitle(story.getLinkTitle())
				.linkDescription(story.getLinkDescription())
				.linkPreviewImage(story.getLinkPreviewImage())
				.createdAt(story.getCreatedAt())
				.viewCount(isOwner ? story.getViewCount() : null)
				.isViewed(isViewed)
				.privacySetting(story.getPrivacy().name())
				.expiresAt(story.getExpiresAt())
				.build();
	}
}
