package com.whatsapp.exception;

import com.whatsapp.entity.ErrorCode;
import lombok.Getter;

@Getter
public class MediaException extends RuntimeException {
    private final ErrorCode errorCode;

    public MediaException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public MediaException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MediaException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
