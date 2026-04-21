package com.whatsapp.repository;

import com.whatsapp.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  @Query("SELECT u FROM User u WHERE "
      + "u.email = :email OR u.phoneNumber = :phone OR u.username = :username")
  Optional<User> findByEmailOrPhoneNumberOrUsername(@Param("email") String email,
      @Param("phone") String phone, @Param("username") String username);

  @Query("SELECT u FROM User u WHERE "
      + "(u.email = :identifier OR u.phoneNumber = :identifier OR u.username = :identifier) "
      + "AND u.status = 'ACTIVE'")
  Optional<User> findByIdentifierAndStatus(@Param("identifier") String identifier);

  @Query("SELECT u FROM User u WHERE "
      + "u.email = :identifier OR u.phoneNumber = :identifier OR u.username = :identifier")
  Optional<User> findByIdentifier(@Param("identifier") String identifier);

  @Query("SELECT u.id as id, u.username as username, u.email as email, u.phoneNumber as phoneNumber, "
      + "u.displayName as displayName, u.profilePictureUrl as profilePictureUrl, u.aboutText as aboutText, "
      + "u.isOnline as isOnline, u.lastSeenAt as lastSeenAt, u.accountType as accountType, "
      + "u.isVerified as isVerified, u.status as status, u.password as password "
      + "FROM User u WHERE u.email = :identifier OR u.phoneNumber = :identifier OR u.username = :identifier")
  Optional<UserBasicProjection> findBasicByIdentifier(@Param("identifier") String identifier);

  @Query("SELECT u FROM User u WHERE "
      + "(LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')) "
      + "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) "
      + "OR u.phoneNumber LIKE CONCAT('%', :query, '%')) "
      + "AND u.status = 'ACTIVE' AND u.id != :excludeUserId")
  List<User> searchUsers(@Param("query") String query, @Param("excludeUserId") Long excludeUserId,
      Pageable pageable);

  @Query("SELECT u FROM User u WHERE u.phoneNumber IN :phoneNumbers AND u.status = 'ACTIVE'")
  List<User> findByPhoneNumbers(@Param("phoneNumbers") List<String> phoneNumbers);

  @Query("SELECT u FROM User u WHERE u.phoneNumber IN :phoneNumbers AND u.status = 'ACTIVE'")
  List<User> findByPhoneNumberIn(@Param("phoneNumbers") List<String> phoneNumbers);

  @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber AND u.status = 'ACTIVE'")
  Optional<User> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

  @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE "
      + "u.email = :email OR u.phoneNumber = :phoneNumber OR u.username = :username")
  boolean existsByEmailOrPhoneNumberOrUsername(@Param("email") String email,
      @Param("phoneNumber") String phoneNumber, @Param("username") String username);

  @Modifying
  @Query("UPDATE User u SET u.isOnline = :isOnline, u.lastSeenAt = :lastSeenAt "
      + "WHERE u.id = :userId")
  int updatePresence(@Param("userId") Long userId, @Param("isOnline") boolean isOnline,
      @Param("lastSeenAt") LocalDateTime lastSeenAt);

  boolean existsByEmail(String email);

  boolean existsByPhoneNumber(String phoneNumber);

  boolean existsByUsername(String username);

  interface UserBasicProjection {
    Long getId();

    String getUsername();

    String getEmail();

    String getPhoneNumber();

    String getDisplayName();

    String getProfilePictureUrl();

    String getAboutText();

    boolean getIsOnline();

    LocalDateTime getLastSeenAt();

    User.AccountType getAccountType();

    boolean getIsVerified();

    User.UserStatus getStatus();

    String getPassword();
  }
}
