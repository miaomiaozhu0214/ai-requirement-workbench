package com.example.airequirementworkbench.common.response;

public record ApiResponse<T>(boolean success, String code, String message, T data) {
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, "OK", "success", data);
  }

  public static <T> ApiResponse<T> fail(String code, String message) {
    return new ApiResponse<>(false, code, message, null);
  }
}
