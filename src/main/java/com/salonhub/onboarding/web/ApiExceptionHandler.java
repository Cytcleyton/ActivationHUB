package com.salonhub.onboarding.web;

import com.salonhub.onboarding.service.OnboardingException;
import com.salonhub.onboarding.web.Dtos.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OnboardingException.class)
    public ResponseEntity<ErrorResponse> onboarding(OnboardingException e) {
        return error(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e) {
        return error(400, "The request body isn't valid JSON.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> badPathValue(MethodArgumentTypeMismatchException e) {
        return error(400, "Invalid " + e.getName() + ": " + e.getValue());
    }

    private static ResponseEntity<ErrorResponse> error(int status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status, message));
    }
}
