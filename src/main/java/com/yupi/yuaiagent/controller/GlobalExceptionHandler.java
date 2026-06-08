package com.yupi.yuaiagent.controller;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NonTransientAiException.class)
    public ResponseEntity<Map<String, String>> handleAiException(NonTransientAiException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "AI 服务调用失败，请检查 DashScope API Key、账号余额或模型服务状态。",
                        "detail", e.getMessage()
                ));
    }
}
