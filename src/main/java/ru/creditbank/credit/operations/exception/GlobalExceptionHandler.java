package ru.creditbank.credit.operations.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Некорректные данные заявки",
                errors
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        String message;
        if (cause instanceof UnrecognizedPropertyException unrecognizedPropertyException) {
            message = "Неизвестное поле в теле запроса: " + unrecognizedPropertyException.getPropertyName();
        } else if (cause instanceof InvalidFormatException invalidFormatException
                && !invalidFormatException.getPath().isEmpty()) {
            message = "Некорректный формат поля: "
                    + invalidFormatException.getPath().get(0).getFieldName();
        } else {
            message = "Тело запроса не может быть прочитано";
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                List.of(message)
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }
}