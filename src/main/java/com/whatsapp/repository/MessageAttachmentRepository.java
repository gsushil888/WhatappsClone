package com.whatsapp.repository;

import com.whatsapp.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
  List<MessageAttachment> findByMessageId(Long messageId);

  @Query("SELECT a FROM MessageAttachment a JOIN FETCH a.message WHERE a.message.id IN :messageIds")
  List<MessageAttachment> findByMessageIdIn(@Param("messageIds") List<Long> messageIds);
}
