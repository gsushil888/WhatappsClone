package com.whatsapp.exception;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.entity.ErrorCode;
import com.whatsapp.util.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex,
      WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();
    log.error("Authentication error [{}]: {} - {}", correlationId, ex.getErrorCode().getCode(),
        ex.getMessage());

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false).message(ex.getMessage())
        .errorCode(ex.getErrorCode().getCode()).correlationId(correlationId)
        .statusCode(ex.getErrorCode().getHttpStatus().value()).timestamp(LocalDateTime.now())
        .build();

    return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
  }

  @ExceptionHandler(UserException.class)
  public ResponseEntity<ApiResponse<Void>> handleUserException(UserException ex,
      WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();
    log.error("User error [{}]: {} - {}", correlationId, ex.getErrorCode().getCode(),
        ex.getMessage());

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false).message(ex.getMessage())
        .errorCode(ex.getErrorCode().getCode()).correlationId(correlationId)
        .statusCode(ex.getErrorCode().getHttpStatus().value()).timestamp(LocalDateTime.now())
        .build();

    return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
  }

  @ExceptionHandler(ConversationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConversationException(ConversationException ex,
      WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();
    log.error("Conversation error [{}]: {} - {}", correlationId, ex.getErrorCode().getCode(),
        ex.getMessage());

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false).message(ex.getMessage())
        .errorCode(ex.getErrorCode().getCode()).correlationId(correlationId)
        .statusCode(ex.getErrorCode().getHttpStatus().value()).timestamp(LocalDateTime.now())
        .build();

    return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
  }

  @ExceptionHandler(MessageException.class)
  public ResponseEntity<ApiResponse<Void>> handleMessageException(MessageException ex,
      WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();
    log.error("Message error [{}]: {} - {}", correlationId, ex.getErrorCode().getCode(),
        ex.getMessage());

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false).message(ex.getMessage())
        .errorCode(ex.getErrorCode().getCode()).correlationId(correlationId)
        .statusCode(ex.getErrorCode().getHttpStatus().value()).timestamp(LocalDateTime.now())
        .build();

    return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
  }

  @ExceptionHandler(ContactException.class)
  public ResponseEntity<ApiResponse<Void>> handleContactException(ContactException ex,
      WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();
    log.error("Contact error [{}]: {} - {}", correlationId, ex.getErrorCode().getCode(),
        ex.getMessage());

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false).message(ex.getMessage())
        .errorCode(ex.getErrorCode().getCode()).correlationId(correlationId)
        .statusCode(ex.getErrorCode().getHttpStatus().value()).timestamp(LocalDateTime.now())
        .build();

    return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
  }

  @ExceptionHandler(StoryException.class)
  public ResponseEntity<ApiResponse<Void>> handleStoryException(StoryException ex,
      WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();
    log.error("Story error [{}]: {} - {}", correlationId, ex.getErrorCode().getCode(),
        ex.getMessage());

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false).message(ex.getMessage())
        .errorCode(ex.getErrorCode().getCode()).correlationId(correlationId)
        .statusCode(ex.getErrorCode().getHttpStatus().value()).timestamp(LocalDateTime.now())
        .build();

    return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
  }

  @ExceptionHandler(CallException.class)
  public ResponseEntity<ApiResponse<Void>> handleCallException(CallException ex,
      WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();
    log.error("Call error [{}]: {} - {}", correlationId, ex.getErrorCode().getCode(),
        ex.getMessage());

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false).message(ex.getMessage())
        .errorCode(ex.getErrorCode().getCode()).correlationId(correlationId)
        .statusCode(ex.getErrorCode().getHttpStatus().value()).timestamp(LocalDateTime.now())
        .build();

    return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
  }

  @ExceptionHandler(MediaException.class)
  public ResponseEntity<ApiResponse<Void>> handleMediaException(MediaException ex,
      WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();
    log.error("Media error [{}]: {} - {}", correlationId, ex.getErrorCode().getCode(),
        ex.getMessage());

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false).message(ex.getMessage())
        .errorCode(ex.getErrorCode().getCode()).correlationId(correlationId)
        .statusCode(ex.getErrorCode().getHttpStatus().value()).timestamp(LocalDateTime.now())
        .build();

    return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex, WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();
    log.error("File upload size exceeded [{}]: {}", correlationId, ex.getMessage());

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false)
        .message("File size exceeds maximum limit of 10MB").errorCode("FILE_001")
        .correlationId(correlationId).statusCode(HttpStatus.BAD_REQUEST.value())
        .timestamp(LocalDateTime.now()).build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(SystemException.class)
  public ResponseEntity<ApiResponse<Void>> handleSystemException(SystemException ex,
      WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();
    log.error("System error [{}]: {} - {}", correlationId, ex.getErrorCode().getCode(),
        ex.getMessage(), ex);

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false).message(ex.getMessage())
        .errorCode(ex.getErrorCode().getCode()).correlationId(correlationId)
        .statusCode(ex.getErrorCode().getHttpStatus().value()).timestamp(LocalDateTime.now())
        .build();

    return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException ex, WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();

    String errorMessage = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));

    log.error("Validation error [{}]: {}", correlationId, errorMessage);

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false).message(errorMessage)
        .errorCode(ErrorCode.SYS_VALIDATION_ERROR.getCode()).correlationId(correlationId)
        .statusCode(ErrorCode.SYS_VALIDATION_ERROR.getHttpStatus().value())
        .timestamp(LocalDateTime.now()).build();

    return ResponseEntity.status(ErrorCode.SYS_VALIDATION_ERROR.getHttpStatus()).body(response);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();

    String errorMessage = ex.getConstraintViolations().stream()
        .map(violation -> violation.getMessage()).collect(Collectors.joining(", "));

    log.error("Constraint violation [{}]: {}", correlationId, errorMessage);

    ApiResponse<Void> response = ApiResponse.<Void>builder().success(false).message(errorMessage)
        .errorCode(ErrorCode.SYS_VALIDATION_ERROR.getCode()).correlationId(correlationId)
        .statusCode(ErrorCode.SYS_VALIDATION_ERROR.getHttpStatus().value())
        .timestamp(LocalDateTime.now()).build();

    return ResponseEntity.status(ErrorCode.SYS_VALIDATION_ERROR.getHttpStatus()).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex,
      WebRequest request) {
    String correlationId = RequestContext.getCorrelationId();
    log.error("Unexpected error [{}]: {}", correlationId, ex.getMessage(), ex);

    ApiResponse<Void> response =
        ApiResponse.<Void>builder().success(false).message("An unexpected error occurred")
            .errorCode(ErrorCode.SYS_INTERNAL_ERROR.getCode()).correlationId(correlationId)
            .statusCode(ErrorCode.SYS_INTERNAL_ERROR.getHttpStatus().value())
            .timestamp(LocalDateTime.now()).build();

    return ResponseEntity.status(ErrorCode.SYS_INTERNAL_ERROR.getHttpStatus()).body(response);
  }
}
