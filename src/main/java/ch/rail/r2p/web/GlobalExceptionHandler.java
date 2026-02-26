package ch.rail.r2p.web;

import jakarta.validation.ConstraintViolationException;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
// make sure that our handler is prioritized over default Spring handlers
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseBody
    public ProblemDetail handleNoSuchElementException(NoSuchElementException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseBody
    public ProblemDetail handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ProblemDetail handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'",
            ex.getValue(), ex.getName());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getAllErrors().stream()
            .map(error -> {
                if (error instanceof FieldError fieldError) {
                    return fieldError.getField() + ": " + error.getDefaultMessage() +
                        (fieldError.getRejectedValue() != null
                            ? " (rejected value: '" + fieldError.getRejectedValue() + "')"
                            : "");
                }
                return error.getDefaultMessage();
            })
            .collect(Collectors.joining("; "));

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed: " + errors);
    }
}
