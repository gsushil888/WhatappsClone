package com.whatsapp.service;

import com.whatsapp.dto.SearchDto;
import com.whatsapp.entity.*;
import com.whatsapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

	private final SearchRepository searchRepository;
	private final ContactRepository contactRepository;
	private final ConversationRepository conversationRepository;
	private final MessageRepository messageRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public SearchDto.UniversalSearchResponse search(Long userId, String query, String type, int limit) {
		log.info("Universal search - userId: {}, query: '{}', type: {}, limit: {}", userId, query, type, limit);
		
		Pageable pageable = PageRequest.of(0, limit);

		SearchDto.UniversalSearchResponse.UniversalSearchResponseBuilder responseBuilder = SearchDto.UniversalSearchResponse
				.builder();

		if ("all".equals(type) || "contacts".equals(type)) {
			List<Contact> contacts = searchRepository.searchContacts(userId, query, pageable);
			log.debug("Found {} contacts for query: '{}'", contacts.size(), query);
			responseBuilder
					.contacts(contacts.stream().map(c -> mapToContactSearchResult(c, userId)).collect(Collectors.toList()));
		}

		if ("all".equals(type) || "chats".equals(type)) {
			List<Conversation> conversations = searchRepository.searchConversations(userId, query, pageable);
			log.debug("Found {} conversations for query: '{}'", conversations.size(), query);
			responseBuilder.conversations(
					conversations.stream().map(c -> mapToConversationSearchResult(c, userId)).collect(Collectors.toList()));
		}

		if ("all".equals(type) || "messages".equals(type)) {
			List<Message> messages = searchRepository.searchMessages(userId, query, pageable);
			log.debug("Found {} messages for query: '{}'", messages.size(), query);
			responseBuilder
					.messages(messages.stream().map(m -> mapToMessageSearchResult(m, userId)).collect(Collectors.toList()));
		}

		if ("all".equals(type) || "users".equals(type)) {
			List<User> users = searchRepository.searchUsers(userId, query, pageable);
			log.debug("Found {} users for query: '{}'", users.size(), query);
			responseBuilder.users(users.stream().map(u -> mapToUserSearchResult(u, userId)).collect(Collectors.toList()));
		}

		SearchDto.UniversalSearchResponse response = responseBuilder.build();

		// Add summary
		response.setSummary(SearchDto.SearchSummary.builder()
				.totalContacts(response.getContacts() != null ? response.getContacts().size() : 0)
				.totalConversations(response.getConversations() != null ? response.getConversations().size() : 0)
				.totalMessages(response.getMessages() != null ? response.getMessages().size() : 0)
				.totalUsers(response.getUsers() != null ? response.getUsers().size() : 0).hasMore(false).build());

		log.info("Universal search completed - userId: {}, query: '{}', total results: {}", 
				userId, query, response.getSummary().getTotalContacts() + response.getSummary().getTotalConversations() + 
				response.getSummary().getTotalMessages() + response.getSummary().getTotalUsers());

		return response;
	}

	@Transactional(readOnly = true)
	public List<SearchDto.ContactSearchResult> searchContacts(Long userId, String query, int limit) {
		Pageable pageable = PageRequest.of(0, limit);
		List<Contact> contacts = searchRepository.searchContacts(userId, query, pageable);

		return contacts.stream().map(c -> mapToContactSearchResult(c, userId)).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<SearchDto.ConversationSearchResult> searchConversations(Long userId, String query, int limit) {
		Pageable pageable = PageRequest.of(0, limit);
		List<Conversation> conversations = searchRepository.searchConversations(userId, query, pageable);

		return conversations.stream().map(c -> mapToConversationSearchResult(c, userId)).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<SearchDto.MessageSearchResult> searchMessages(Long userId, String query, int limit) {
		Pageable pageable = PageRequest.of(0, limit);
		List<Message> messages = searchRepository.searchMessages(userId, query, pageable);

		return messages.stream().map(m -> mapToMessageSearchResult(m, userId)).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<SearchDto.UserSearchResult> searchUsers(Long userId, String query, int limit) {
		Pageable pageable = PageRequest.of(0, limit);
		List<User> users = searchRepository.searchUsers(userId, query, pageable);

		return users.stream().map(u -> mapToUserSearchResult(u, userId)).collect(Collectors.toList());
	}

	private SearchDto.ContactSearchResult mapToContactSearchResult(Contact contact, Long currentUserId) {
		String displayName = getDisplayName(currentUserId, contact.getContactUser());
		return SearchDto.ContactSearchResult.builder().id(contact.getId()).type("CONTACT")
				.contactUser(SearchDto.ContactUser.builder().id(contact.getContactUser().getId())
						.displayName(displayName)
						.profilePictureUrl(contact.getContactUser().getProfilePictureUrl())
						.isOnline(contact.getContactUser().isOnline()).build())
				.displayName(contact.getDisplayName()).matchedField("displayName").build();
	}

	private SearchDto.ConversationSearchResult mapToConversationSearchResult(Conversation conversation, Long currentUserId) {
		return SearchDto.ConversationSearchResult.builder().id(conversation.getId()).type("CONVERSATION")
				.name(conversation.getName()).profileImageUrl(conversation.getGroupImageUrl())
				.conversationType(conversation.getType().name())
				.matchedField("name").build();
	}

	private SearchDto.MessageSearchResult mapToMessageSearchResult(Message message, Long currentUserId) {
		String senderDisplayName = getDisplayName(currentUserId, message.getSender());
		return SearchDto.MessageSearchResult.builder().id(message.getId()).type("MESSAGE").content(message.getContent())
				.timestamp(message.getCreatedAt())
				.conversation(SearchDto.MessageConversation.builder().id(message.getConversation().getId())
						.name(message.getConversation().getName()).type(message.getConversation().getType().name())
						.build())
				.sender(SearchDto.MessageSender.builder().id(message.getSender().getId())
						.displayName(senderDisplayName).build())
				.matchedField("content").build();
	}

	private SearchDto.UserSearchResult mapToUserSearchResult(User user, Long currentUserId) {
		String displayName = getDisplayName(currentUserId, user);
		return SearchDto.UserSearchResult.builder().id(user.getId()).type("USER").username(user.getUsername())
				.displayName(displayName).profilePictureUrl(user.getProfilePictureUrl())
				.isOnline(user.isOnline()).accountType(user.getAccountType().name()).matchedField("displayName")
				.build();
	}

	private String getDisplayName(Long currentUserId, User targetUser) {
		Optional<Contact> contact = contactRepository
				.findByUserIdAndContactUserId(currentUserId, targetUser.getId());
		if (contact.isPresent()) {
			return contact.get().getDisplayName();
		}
		String userDisplayName = targetUser.getDisplayName();
		return (userDisplayName != null && !userDisplayName.trim().isEmpty()) 
				? userDisplayName : targetUser.getPhoneNumber();
	}
}
