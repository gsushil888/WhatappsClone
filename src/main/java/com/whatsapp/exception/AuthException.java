package com.whatsapp.exception;

import com.whatsapp.entity.ErrorCode;
import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final ErrorCode errorCode;

  public AuthException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public AuthException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public AuthException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }
}
