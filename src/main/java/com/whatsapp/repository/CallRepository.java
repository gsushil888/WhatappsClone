package com.whatsapp.repository;

import com.whatsapp.entity.Call;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CallRepository extends JpaRepository<Call, Long> {

  @Query("SELECT c FROM Call c JOIN FETCH c.participants cp " + "WHERE cp.user.id = :userId "
      + "ORDER BY c.startedAt DESC")
  List<Call> findUserCallHistory(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT c FROM Call c JOIN c.participants cp "
      + "WHERE c.id = :callId AND cp.user.id = :userId")
  Optional<Call> findByIdAndUserId(@Param("callId") Long callId, @Param("userId") Long userId);

  @Query("SELECT COUNT(c) FROM Call c JOIN c.participants cp "
      + "WHERE cp.user.id = :userId AND c.startedAt >= :since")
  long countUserCallsSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

  @Query("SELECT c FROM Call c " + "WHERE c.status IN ('INITIATED', 'RINGING') "
      + "AND c.startedAt < :timeout")
  List<Call> findTimedOutCalls(@Param("timeout") LocalDateTime timeout);

  @Query("SELECT SUM(c.durationSeconds) FROM Call c JOIN c.participants cp "
      + "WHERE cp.user.id = :userId AND c.status = 'ENDED' AND c.startedAt >= :since")
  Long getTotalCallDuration(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
