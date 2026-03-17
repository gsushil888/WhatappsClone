package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id", nullable = false)
	private Message message;

	@Enumerated(EnumType.STRING)
	private AttachmentType type;

	private String fileName;
	private String fileUrl;
	private String thumbnailUrl;
	private Long fileSize;
	private String mimeType;
	private Integer width;
	private Integer height;
	private Integer duration;
	private String metadata;

	@CreationTimestamp
	private LocalDateTime createdAt;

	public enum AttachmentType {
		IMAGE, VIDEO, AUDIO, DOCUMENT, STICKER
	}
}