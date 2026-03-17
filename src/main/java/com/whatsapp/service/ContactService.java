package com.whatsapp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whatsapp.dto.UserDto;
import com.whatsapp.entity.Contact;
import com.whatsapp.entity.ErrorCode;
import com.whatsapp.entity.User;
import com.whatsapp.exception.UserException;
import com.whatsapp.repository.ContactRepository;
import com.whatsapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDto.ContactListResponse getUserContacts(Long userId, boolean favoritesOnly, boolean onlineOnly,
            int page, int limit, Map<String, String> deviceInfo, Map<String, String> headers) {
        
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit);
        List<Contact> contacts = contactRepository.findUserContacts(userId, pageable);
        
        List<UserDto.ContactResponse> contactResponses = contacts.stream()
                .filter(contact -> !favoritesOnly || contact.isFavorite())
                .filter(contact -> !onlineOnly || contact.getContactUser().isOnline())
                .map(this::mapToContactResponse)
                .collect(Collectors.toList());
        
        long totalCount = contactRepository.countUserContacts(userId);
        
        return UserDto.ContactListResponse.builder()
                .contacts(contactResponses)
                .totalCount((int) totalCount)
                .pagination(UserDto.PaginationInfo.builder()
                        .page(page)
                        .limit(limit)
                        .hasNext(contactResponses.size() == limit)
                        .build())
                .build();
    }

    @Transactional
    public UserDto.ContactResponse addContact(Long userId, UserDto.AddContactRequest request, 
            Map<String, String> deviceInfo, Map<String, String> headers) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // Find user by phone number instead of user ID
        User contactUser = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new UserException(ErrorCode.USER_CONTACT_NOT_FOUND, 
                    "User with phone number " + request.getPhoneNumber() + " is not registered on WhatsApp"));

        // Check if user is trying to add themselves
        if (contactUser.getId().equals(userId)) {
            throw new UserException(ErrorCode.INVALID_OPERATION, "Cannot add yourself as a contact");
        }

        // Check if contact already exists
        if (contactRepository.existsByUserIdAndContactUserId(userId, contactUser.getId())) {
            throw new UserException(ErrorCode.CONTACT_ALREADY_EXISTS, "User is already in your contacts");
        }

        Contact contact = Contact.builder()
                .user(user)
                .contactUser(contactUser)
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : contactUser.getDisplayName())
                .status(Contact.ContactStatus.ACTIVE)
                .build();

        contact = contactRepository.save(contact);
        return mapToContactResponse(contact);
    }

    @Transactional
    public UserDto.ContactResponse updateContact(Long userId, Long contactId, UserDto.UpdateContactRequest request) {
        Contact contact = contactRepository.findByUserIdAndContactUserId(userId, contactId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_CONTACT_NOT_FOUND));

        if (request.getDisplayName() != null) {
            contact.setDisplayName(request.getDisplayName());
        }
        if (request.getIsFavorite() != null) {
            contact.setFavorite(request.getIsFavorite());
        }

        contact = contactRepository.save(contact);
        return mapToContactResponse(contact);
    }

    @Transactional
    public void blockContact(Long userId, Long contactId) {
        Contact contact = contactRepository.findByUserIdAndContactUserId(userId, contactId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_CONTACT_NOT_FOUND));

        contact.setBlocked(true);
        contactRepository.save(contact);
    }

    @Transactional
    public void unblockContact(Long userId, Long contactId) {
        Contact contact = contactRepository.findByUserIdAndContactUserId(userId, contactId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_CONTACT_NOT_FOUND));

        contact.setBlocked(false);
        contactRepository.save(contact);
    }

    @Transactional(readOnly = true)
    public UserDto.ContactListResponse searchContacts(Long userId, String query, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        List<Contact> contacts = contactRepository.searchContacts(userId, query, pageable);
        
        List<UserDto.ContactResponse> contactResponses = contacts.stream()
                .map(this::mapToContactResponse)
                .collect(Collectors.toList());
        
        return UserDto.ContactListResponse.builder()
                .contacts(contactResponses)
                .totalCount(contactResponses.size())
                .pagination(UserDto.PaginationInfo.builder()
                        .page(offset / limit + 1)
                        .limit(limit)
                        .hasNext(contactResponses.size() == limit)
                        .build())
                .build();
    }

    @Transactional
    public UserDto.SyncContactsResponse syncContacts(Long userId, UserDto.SyncContactsRequest request) {
        List<User> foundUsers = userRepository.findByPhoneNumberIn(request.getPhoneNumbers());
        
        List<UserDto.ContactResponse> foundContacts = foundUsers.stream()
                .filter(user -> !user.getId().equals(userId))
                .map(user -> {
                    // Check if already a contact
                    boolean isExistingContact = contactRepository.existsByUserIdAndContactUserId(userId, user.getId());
                    
                    if (!isExistingContact) {
                        // Auto-add as contact
                        Contact contact = Contact.builder()
                                .user(userRepository.findById(userId).orElseThrow())
                                .contactUser(user)
                                .displayName(user.getDisplayName())
                                .status(Contact.ContactStatus.ACTIVE)
                                .build();
                        contactRepository.save(contact);
                    }
                    
                    return UserDto.ContactResponse.builder()
                            .contactUserId(user.getId())
                            .displayName(user.getDisplayName())
                            .phoneNumber(user.getPhoneNumber())
                            .profilePictureUrl(user.getProfilePictureUrl())
                            .isOnline(user.isOnline())
                            .build();
                })
                .collect(Collectors.toList());
        
        List<String> foundNumbers = foundUsers.stream()
                .map(User::getPhoneNumber)
                .collect(Collectors.toList());
        
        List<String> notFoundNumbers = request.getPhoneNumbers().stream()
                .filter(phone -> !foundNumbers.contains(phone))
                .collect(Collectors.toList());
        
        return UserDto.SyncContactsResponse.builder()
                .foundContacts(foundContacts)
                .notFoundNumbers(notFoundNumbers)
                .syncedAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto.ContactListResponse getBlockedContacts(Long userId, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        List<Contact> contacts = contactRepository.findBlockedContacts(userId, pageable);
        
        List<UserDto.ContactResponse> contactResponses = contacts.stream()
                .map(this::mapToContactResponse)
                .collect(Collectors.toList());
        
        return UserDto.ContactListResponse.builder()
                .contacts(contactResponses)
                .totalCount(contactResponses.size())
                .pagination(UserDto.PaginationInfo.builder()
                        .page(offset / limit + 1)
                        .limit(limit)
                        .hasNext(contactResponses.size() == limit)
                        .build())
                .build();
    }

    @Transactional
    public void deleteContact(Long userId, Long contactId) {
        Contact contact = contactRepository.findByUserIdAndContactUserId(userId, contactId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_CONTACT_NOT_FOUND));
        
        contact.setStatus(Contact.ContactStatus.DELETED);
        contactRepository.save(contact);
    }

    private UserDto.ContactResponse mapToContactResponse(Contact contact) {
        User contactUser = contact.getContactUser();
        return UserDto.ContactResponse.builder()
                .id(contact.getId())
                .contactUserId(contactUser.getId())
                .displayName(contact.getDisplayName())
                .phoneNumber(contactUser.getPhoneNumber())
                .profilePictureUrl(contactUser.getProfilePictureUrl())
                .isOnline(contactUser.isOnline())
                .isFavorite(contact.isFavorite())
                .isBlocked(contact.isBlocked())
                .lastActiveAt(contactUser.getLastActiveAt())
                .createdAt(contact.getCreatedAt())
                .build();
    }
}