package com.whatsapp.exception;

import com.whatsapp.entity.ErrorCode;
import lombok.Getter;

@Getter
public class ContactException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final ErrorCode errorCode;

  public ContactException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public ContactException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ContactException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }
}
