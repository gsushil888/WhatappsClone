package com.whatsapp.repository;

import com.whatsapp.entity.Contact;
import com.whatsapp.entity.Conversation;
import com.whatsapp.entity.Message;
import com.whatsapp.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchRepository extends JpaRepository<User, Long> {

    // Universal search across all entities
    @Query("SELECT c FROM Contact c JOIN FETCH c.contactUser cu " +
           "WHERE c.user.id = :userId AND c.status = 'ACTIVE' " +
           "AND (LOWER(c.displayName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(cu.displayName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR cu.phoneNumber LIKE CONCAT('%', :query, '%'))")
    List<Contact> searchContacts(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN c.participants cp " +
           "WHERE cp.user.id = :userId AND cp.status = 'ACTIVE' " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Conversation> searchConversations(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);

    @Query("SELECT m FROM Message m " +
           "JOIN m.conversation c " +
           "JOIN c.participants cp " +
           "WHERE cp.user.id = :userId AND cp.status = 'ACTIVE' " +
           "AND m.isDeleted = false " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY m.timestamp DESC")
    List<Message> searchMessages(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);

    @Query("SELECT u FROM User u " +
           "WHERE u.status = 'ACTIVE' AND u.id != :userId " +
           "AND (LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR u.phoneNumber LIKE CONCAT('%', :query, '%'))")
    List<User> searchUsers(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);

    // Count queries for pagination
    @Query("SELECT COUNT(c) FROM Contact c JOIN c.contactUser cu " +
           "WHERE c.user.id = :userId AND c.status = 'ACTIVE' " +
           "AND (LOWER(c.displayName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(cu.displayName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR cu.phoneNumber LIKE CONCAT('%', :query, '%'))")
    long countSearchContacts(@Param("userId") Long userId, @Param("query") String query);

    @Query("SELECT COUNT(DISTINCT c) FROM Conversation c " +
           "JOIN c.participants cp " +
           "WHERE cp.user.id = :userId AND cp.status = 'ACTIVE' " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    long countSearchConversations(@Param("userId") Long userId, @Param("query") String query);

    @Query("SELECT COUNT(m) FROM Message m " +
           "JOIN m.conversation c " +
           "JOIN c.participants cp " +
           "WHERE cp.user.id = :userId AND cp.status = 'ACTIVE' " +
           "AND m.isDeleted = false " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    long countSearchMessages(@Param("userId") Long userId, @Param("query") String query);

    @Query("SELECT COUNT(u) FROM User u " +
           "WHERE u.status = 'ACTIVE' AND u.id != :userId " +
           "AND (LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR u.phoneNumber LIKE CONCAT('%', :query, '%'))")
    long countSearchUsers(@Param("userId") Long userId, @Param("query") String query);
}