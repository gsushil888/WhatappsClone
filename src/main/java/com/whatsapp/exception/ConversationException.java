package com.whatsapp.exception;

import com.whatsapp.entity.ErrorCode;
import lombok.Getter;

@Getter
public class ConversationException extends RuntimeException {
    private final ErrorCode errorCode;

    public ConversationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ConversationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ConversationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}