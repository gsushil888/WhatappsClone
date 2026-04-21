package com.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  @Builder.Default
  private Boolean success = true;

  private String message;

  private String errorCode;

  private T data;

  private String correlationId;

  private Integer statusCode;

  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();

  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder().success(true).data(data).build();
  }

  public static <T> ApiResponse<T> success(T data, String message) {
    return ApiResponse.<T>builder().success(true).message(message).data(data).build();
  }

  public static <T> ApiResponse<T> error(String errorCode, String message) {
    return ApiResponse.<T>builder().success(false).errorCode(errorCode).message(message).build();
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class PaginationInfo {
    private Integer page;
    private Integer limit;
    private Integer total;
    private Boolean hasNext;
    private Boolean hasPrev;
    private Integer totalPages;
  }
}
