package com.ecom.notifications.exceptionhandlers;

import com.ecom.common.dtos.ErrorDto;
import com.ecom.notifications.auth.AuthServiceUnavailableException;
import com.ecom.notifications.auth.UnauthorizedException;
import com.ecom.notifications.clients.UserServiceException;
import com.ecom.notifications.exceptions.InvalidEventException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class NotificationExceptionHandler {
    @ExceptionHandler(InvalidEventException.class)
    public ResponseEntity<ErrorDto> handleInvalidEvent(InvalidEventException e) {
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorDto> handleUnauthorized(UnauthorizedException e) {
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({AuthServiceUnavailableException.class, UserServiceException.class})
    public ResponseEntity<ErrorDto> handleServiceUnavailable(RuntimeException e) {
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.SERVICE_UNAVAILABLE);
    }
}
