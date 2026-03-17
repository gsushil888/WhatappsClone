package com.whatsapp.repository;

import com.whatsapp.entity.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPresenceRepository extends JpaRepository<UserPresence, Long> {

    Optional<UserPresence> findByUserId(Long userId);

    @Query("SELECT p FROM UserPresence p WHERE p.status = 'ONLINE'")
    List<UserPresence> findOnlineUsers();

    @Query("SELECT p FROM UserPresence p WHERE p.lastSeen < :threshold")
    List<UserPresence> findUsersOfflineSince(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(p) FROM UserPresence p WHERE p.status = 'ONLINE'")
    long countOnlineUsers();
}