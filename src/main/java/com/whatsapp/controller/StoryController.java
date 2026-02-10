package com.whatsapp.controller;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.StoryDto;
import com.whatsapp.service.StoryService;
import com.whatsapp.util.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

	private final StoryService storyService;

	@GetMapping("/feed")
	public ResponseEntity<ApiResponse<StoryDto.StoryFeedResponse>> getStoryFeed(Authentication authentication,
			@RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "0") int offset) {

		Long userId = (Long) authentication.getPrincipal();
		StoryDto.StoryFeedResponse feed = storyService.getStoryFeed(userId, limit, offset);

		return ResponseEntity.ok(ApiResponse.<StoryDto.StoryFeedResponse>builder().success(true).data(feed)
				.correlationId(RequestContext.getCorrelationId()).build());
	}

	@PostMapping
	public ResponseEntity<ApiResponse<StoryDto.StoryResponse>> postStory(Authentication authentication,
			@Valid @RequestBody StoryDto.PostStoryRequest request) {

		Long userId = (Long) authentication.getPrincipal();
		StoryDto.StoryResponse story = storyService.postStory(userId, request);

		return ResponseEntity.ok(ApiResponse.<StoryDto.StoryResponse>builder().success(true).data(story)
				.correlationId(RequestContext.getCorrelationId()).message("Story posted successfully").build());
	}

	@PostMapping("/{storyId}/view")
	public ResponseEntity<ApiResponse<Void>> viewStory(Authentication authentication, @PathVariable Long storyId) {

		Long userId = (Long) authentication.getPrincipal();
		storyService.viewStory(userId, storyId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId()).message("Story viewed successfully").build());
	}

	@GetMapping("/{storyId}/viewers")
	public ResponseEntity<ApiResponse<StoryDto.StoryViewersResponse>> getStoryViewers(Authentication authentication,
			@PathVariable Long storyId) {

		Long userId = (Long) authentication.getPrincipal();
		StoryDto.StoryViewersResponse viewers = storyService.getStoryViewers(userId, storyId);

		return ResponseEntity.ok(ApiResponse.<StoryDto.StoryViewersResponse>builder().success(true).data(viewers)
				.correlationId(RequestContext.getCorrelationId()).build());
	}

	@DeleteMapping("/{storyId}")
	public ResponseEntity<ApiResponse<Void>> deleteStory(Authentication authentication, @PathVariable Long storyId) {

		Long userId = (Long) authentication.getPrincipal();
		storyService.deleteStory(userId, storyId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId()).message("Story deleted successfully").build());
	}

	@GetMapping("/my-stories")
	public ResponseEntity<ApiResponse<StoryDto.StoryFeedResponse>> getMyStories(Authentication authentication,
			@RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "0") int offset) {

		Long userId = (Long) authentication.getPrincipal();
		StoryDto.StoryFeedResponse stories = storyService.getMyStories(userId, limit, offset);

		return ResponseEntity.ok(ApiResponse.<StoryDto.StoryFeedResponse>builder().success(true).data(stories)
				.correlationId(RequestContext.getCorrelationId()).build());
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<ApiResponse<StoryDto.UserStoriesResponse>> getUserStories(Authentication authentication,
			@PathVariable Long userId,
			@RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "0") int offset) {

		Long currentUserId = (Long) authentication.getPrincipal();
		StoryDto.UserStoriesResponse stories = storyService.getUserStories(currentUserId, userId, limit, offset);

		return ResponseEntity.ok(ApiResponse.<StoryDto.UserStoriesResponse>builder().success(true).data(stories)
				.correlationId(RequestContext.getCorrelationId()).build());
	}
}