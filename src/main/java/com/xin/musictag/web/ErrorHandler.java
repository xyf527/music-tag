package com.xin.musictag.web;

import com.xin.musictag.domain.ProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(ProcessingException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String,String> processing(ProcessingException e) { return Map.of("errorCode", e.code(), "stage", e.stage(), "message", e.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> badRequest(IllegalArgumentException e) { return Map.of("errorCode", "INVALID_REQUEST", "stage", "REQUEST", "message", e.getMessage()); }
}
