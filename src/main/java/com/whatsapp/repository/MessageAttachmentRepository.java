package com.whatsapp.repository;

import com.whatsapp.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
	List<MessageAttachment> findByMessageId(Long messageId);
}
