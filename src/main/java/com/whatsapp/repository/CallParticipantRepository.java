package com.whatsapp.repository;

import com.whatsapp.entity.CallParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallParticipantRepository extends JpaRepository<CallParticipant, Long> {
}
