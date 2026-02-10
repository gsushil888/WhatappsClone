package com.whatsapp.repository;

import com.whatsapp.entity.Contact;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    @Query("SELECT c FROM Contact c JOIN FETCH c.contactUser cu " +
           "WHERE c.user.id = :userId AND c.status = 'ACTIVE' " +
           "ORDER BY c.isFavorite DESC, cu.displayName ASC")
    List<Contact> findUserContacts(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Contact c JOIN FETCH c.contactUser cu " +
           "WHERE c.user.id = :userId AND cu.id = :contactUserId AND c.status = 'ACTIVE'")
    Optional<Contact> findByUserIdAndContactUserId(@Param("userId") Long userId, @Param("contactUserId") Long contactUserId);

    @Query("SELECT c FROM Contact c JOIN FETCH c.contactUser cu " +
           "WHERE c.user.id = :userId AND c.status = 'ACTIVE' " +
           "AND (LOWER(c.displayName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(cu.displayName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR cu.phoneNumber LIKE CONCAT('%', :query, '%'))")
    List<Contact> searchContacts(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);

    @Query("SELECT c FROM Contact c JOIN FETCH c.contactUser cu " +
           "WHERE c.user.id = :userId AND c.isBlocked = true " +
           "ORDER BY c.updatedAt DESC")
    List<Contact> findBlockedContacts(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Contact c JOIN FETCH c.contactUser cu " +
           "WHERE c.user.id = :userId AND c.isFavorite = true AND c.status = 'ACTIVE' " +
           "ORDER BY cu.displayName ASC")
    List<Contact> findFavoriteContacts(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM Contact c WHERE c.user.id = :userId AND c.status = 'ACTIVE'")
    long countUserContacts(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Contact c SET c.isBlocked = :blocked, c.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE c.id = :contactId AND c.user.id = :userId")
    int updateBlockStatus(@Param("contactId") Long contactId, @Param("userId") Long userId, @Param("blocked") boolean blocked);

    boolean existsByUserIdAndContactUserId(Long userId, Long contactUserId);
}