package com.minio.storage.exception;

import com.minio.storage.enums.FileCategory;
import com.minio.storage.util.ActivityLogHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class GlobalExceptionHandler {

    private final ActivityLogHelper activityLogHelper;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(
            AppException ex,
            HttpServletRequest request) {

        if (ex.getStatus() == HttpStatus.UNAUTHORIZED && ex.getUsername() != null) {
            log.warn("Detektovana neuspješna prijava za korisnika: {} - Razlog: {}", ex.getUsername(), ex.getMessage());

            activityLogHelper.logLoginFailure(
                    ex.getUsername(),
                    ex.getMessage(),
                    request
            );
        }

        ErrorResponse error = ErrorResponse.of(
                ex.getStatus(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            fieldErrors.put(fieldName, error.getDefaultMessage());
        });

        log.warn("Validacija nije prošla za zahtjev na: {}", request.getRequestURI());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "Poslati podaci nisu ispravni",
                request.getRequestURI()
        );
        error.setFieldErrors(fieldErrors);

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Kritična greška: Neočekivan izuzetak na {}", request.getRequestURI(), ex);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Došlo je do neočekivane greške.",
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex,
            HttpServletRequest request) {

        String message = ex.getConstraintViolations().iterator().next().getMessage();

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleEnumMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String allowed = Arrays.stream(FileCategory.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        String message = "Nevalidna vrijednost '" + ex.getValue() + "'. Dozvoljene vrijednosti: " + allowed;

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}