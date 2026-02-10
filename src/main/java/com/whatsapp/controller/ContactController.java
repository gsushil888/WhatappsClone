package com.whatsapp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.UserDto;
import com.whatsapp.service.ContactService;
import com.whatsapp.util.RequestContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {

	private final ContactService contactService;

	@GetMapping
	public ResponseEntity<ApiResponse<UserDto.ContactListResponse>> getContacts(
			Authentication authentication,
			@RequestParam(defaultValue = "false") boolean favoritesOnly,
			@RequestParam(defaultValue = "false") boolean onlineOnly,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "50") int limit,
			@RequestHeader Map<String, String> headers) {

		Long userId = (Long) authentication.getPrincipal();
		log.info("Contacts of user => "+userId);
		Map<String, String> deviceInfo = RequestContext.getDeviceInfo();
		UserDto.ContactListResponse contacts = contactService.getUserContacts(userId, favoritesOnly, onlineOnly,
				page, limit, deviceInfo, headers);

		return ResponseEntity.ok(ApiResponse.<UserDto.ContactListResponse>builder().success(true).data(contacts)
				.correlationId(RequestContext.getCorrelationId()).build());
	}

	@PostMapping
	public ResponseEntity<ApiResponse<UserDto.ContactResponse>> addContact(
			Authentication authentication,
			@Valid @RequestBody UserDto.AddContactRequest request,
			@RequestHeader Map<String, String> headers) {

		Long userId = (Long) authentication.getPrincipal();
		Map<String, String> deviceInfo = RequestContext.getDeviceInfo();
		UserDto.ContactResponse contact = contactService.addContact(userId, request, deviceInfo, headers);

		return ResponseEntity.ok(ApiResponse.<UserDto.ContactResponse>builder().success(true).data(contact)
				.correlationId(RequestContext.getCorrelationId()).message("Contact added successfully").build());
	}

	@PutMapping("/{contactId}")
	public ResponseEntity<ApiResponse<UserDto.ContactResponse>> updateContact(Authentication authentication,
			@PathVariable Long contactId, @Valid @RequestBody UserDto.UpdateContactRequest request) {

		Long userId = (Long) authentication.getPrincipal();
		UserDto.ContactResponse contact = contactService.updateContact(userId, contactId, request);

		return ResponseEntity.ok(ApiResponse.<UserDto.ContactResponse>builder().success(true).data(contact)
				.correlationId(RequestContext.getCorrelationId()).message("Contact updated successfully").build());
	}

	@PostMapping("/{contactId}/block")
	public ResponseEntity<ApiResponse<Void>> blockContact(Authentication authentication,
			@PathVariable Long contactId) {

		Long userId = (Long) authentication.getPrincipal();
		contactService.blockContact(userId, contactId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId()).message("Contact blocked successfully").build());
	}

	@PostMapping("/{contactId}/unblock")
	public ResponseEntity<ApiResponse<Void>> unblockContact(Authentication authentication,
			@PathVariable Long contactId) {

		Long userId = (Long) authentication.getPrincipal();
		contactService.unblockContact(userId, contactId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId()).message("Contact unblocked successfully").build());
	}

	@GetMapping("/search")
	public ResponseEntity<ApiResponse<UserDto.ContactListResponse>> searchContacts(Authentication authentication,
			@RequestParam String q, 
			@RequestParam(defaultValue = "20") int limit,
			@RequestParam(defaultValue = "0") int offset) {

		Long userId = (Long) authentication.getPrincipal();
		log.info("Search Contact => "+q);
		UserDto.ContactListResponse contacts = contactService.searchContacts(userId, q, limit, offset);

		return ResponseEntity.ok(ApiResponse.<UserDto.ContactListResponse>builder().success(true).data(contacts)
				.correlationId(RequestContext.getCorrelationId()).build());
	}

	@PostMapping("/sync")
	public ResponseEntity<ApiResponse<UserDto.SyncContactsResponse>> syncContacts(Authentication authentication,
			@Valid @RequestBody UserDto.SyncContactsRequest request) {

		Long userId = (Long) authentication.getPrincipal();
		UserDto.SyncContactsResponse response = contactService.syncContacts(userId, request);

		return ResponseEntity.ok(ApiResponse.<UserDto.SyncContactsResponse>builder().success(true).data(response)
				.correlationId(RequestContext.getCorrelationId()).message("Contacts synced successfully").build());
	}

	@GetMapping("/blocked")
	public ResponseEntity<ApiResponse<UserDto.ContactListResponse>> getBlockedContacts(
			Authentication authentication,
			@RequestParam(defaultValue = "20") int limit,
			@RequestParam(defaultValue = "0") int offset) {

		Long userId = (Long) authentication.getPrincipal();
		UserDto.ContactListResponse contacts = contactService.getBlockedContacts(userId, limit, offset);

		return ResponseEntity.ok(ApiResponse.<UserDto.ContactListResponse>builder().success(true).data(contacts)
				.correlationId(RequestContext.getCorrelationId()).build());
	}

	@DeleteMapping("/{contactId}")
	public ResponseEntity<ApiResponse<Void>> deleteContact(Authentication authentication,
			@PathVariable Long contactId) {

		Long userId = (Long) authentication.getPrincipal();
		contactService.deleteContact(userId, contactId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId()).message("Contact deleted successfully").build());
	}
}