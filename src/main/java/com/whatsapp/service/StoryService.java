package com.whatsapp.service;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.StoryDto;
import com.whatsapp.entity.*;
import com.whatsapp.exception.UserException;
import com.whatsapp.exception.SystemException;
import com.whatsapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

	private final StoryRepository storyRepository;
	private final UserRepository userRepository;
	private final ContactRepository contactRepository;

	@Transactional(readOnly = true)
	public StoryDto.StoryFeedResponse getStoryFeed(Long userId, int limit, int offset) {
		Pageable pageable = PageRequest.of(offset / limit, limit);
		List<Story> stories = storyRepository.findStoryFeed(userId, pageable);

		List<StoryDto.StoryResponse> storyResponses = stories.stream().map(this::mapToStoryResponse)
				.collect(Collectors.toList());

		return StoryDto.StoryFeedResponse.builder().stories(storyResponses)
				.pagination(ApiResponse.PaginationInfo.builder().page(offset / limit + 1).limit(limit)
						.total(storyResponses.size()).hasNext(storyResponses.size() == limit).build())
				.build();
	}

	@Transactional
	public StoryDto.StoryResponse postStory(Long userId, StoryDto.PostStoryRequest request) {
		User user = userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		Story story = Story.builder().user(user).type(Story.StoryType.valueOf(request.getStoryType().toUpperCase()))
				.content(request.getContent()).mediaUrl(request.getMediaUrl())
				.privacy(Story.StoryPrivacy.valueOf(request.getPrivacySetting().toUpperCase()))
				.expiresAt(LocalDateTime.now().plusHours(24)).viewCount(0).isActive(true).build();

		story = storyRepository.save(story);
		return mapToStoryResponse(story);
	}

	@Transactional
	public void viewStory(Long userId, Long storyId) {
		Story story = storyRepository.findById(storyId).orElseThrow(() -> new UserException(ErrorCode.STORY_NOT_FOUND));

		if (story.isExpired()) {
			throw new UserException(ErrorCode.STORY_EXPIRED);
		}

		// Increment view count
		story.setViewCount(story.getViewCount() + 1);
		storyRepository.save(story);

		log.info("Story {} viewed by user {}", storyId, userId);
	}

	@Transactional(readOnly = true)
	public StoryDto.StoryViewersResponse getStoryViewers(Long userId, Long storyId) {
		Story story = storyRepository.findByIdAndUserId(storyId, userId)
				.orElseThrow(() -> new UserException(ErrorCode.STORY_NOT_FOUND));

		// Mock implementation - in real app, would fetch from StoryView entity
		return StoryDto.StoryViewersResponse.builder().viewers(List.of()).totalViewers(story.getViewCount()).build();
	}

	@Transactional
	public void deleteStory(Long userId, Long storyId) {
		Story story = storyRepository.findByIdAndUserId(storyId, userId)
				.orElseThrow(() -> new UserException(ErrorCode.STORY_NOT_FOUND));

		story.setIsActive(false);
		storyRepository.save(story);

		log.info("Story {} deleted by user {}", storyId, userId);
	}

	@Transactional(readOnly = true)
	public StoryDto.StoryFeedResponse getMyStories(Long userId, int limit, int offset) {
		Pageable pageable = PageRequest.of(offset / limit, limit);
		List<Story> stories = storyRepository.findUserStories(userId, pageable);

		List<StoryDto.StoryResponse> storyResponses = stories.stream().filter(story -> !story.isExpired())
				.map(this::mapToStoryResponse).collect(Collectors.toList());

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
				.filter(story -> !story.isExpired() && canViewStory(viewerId, story)).map(this::mapToStoryResponse)
				.collect(Collectors.toList());

		return StoryDto.UserStoriesResponse.builder().userId(userId).userName(user.getDisplayName())
				.userAvatar(user.getProfilePictureUrl()).stories(storyResponses).unviewedCount(storyResponses.size())
				.lastStoryAt(stories.isEmpty() ? null : stories.get(0).getCreatedAt()).build();
	}

	@Transactional
	public void cleanupExpiredStories() {
		int deletedCount = storyRepository.deleteExpiredStories();
		log.info("Cleaned up {} expired stories", deletedCount);
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
			// Would need additional logic for close friends and custom privacy
			return false;
		default:
			return false;
		}
	}

	private StoryDto.StoryResponse mapToStoryResponse(Story story) {
		return StoryDto.StoryResponse.builder().id(story.getId()).userId(story.getUser().getId())
				.userName(story.getUser().getDisplayName()).userAvatar(story.getUser().getProfilePictureUrl())
				.content(story.getContent()).mediaUrl(story.getMediaUrl()).storyType(story.getType().name())
				.createdAt(story.getCreatedAt()).viewCount(story.getViewCount()).isViewed(false) // Would need to check
																									// against StoryView
																									// entity
				.privacySetting(story.getPrivacy().name()).expiresAt(story.getExpiresAt()).build();
	}
}