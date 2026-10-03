package com.xin.musictag.web;

import com.xin.musictag.application.UploadLimits;
import com.xin.musictag.domain.ProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {
    private final UploadLimits limits;

    public ErrorHandler(UploadLimits limits) { this.limits = limits; }
    @ExceptionHandler(ProcessingException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String,String> processing(ProcessingException e) { return Map.of("errorCode", e.code(), "stage", e.stage(), "message", e.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> badRequest(IllegalArgumentException e) { return Map.of("errorCode", "INVALID_REQUEST", "stage", "REQUEST", "message", e.getMessage()); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Map<String,String> uploadTooLarge(MaxUploadSizeExceededException e) { return Map.of("errorCode", "UPLOAD_TOO_LARGE", "stage", "UPLOAD", "message", "上传文件超过允许的大小上限 " + limits.maxAudioMegabytes()); }
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> multipart(MultipartException e) { return Map.of("errorCode", "INVALID_UPLOAD", "stage", "UPLOAD", "message", "上传请求无效，请检查音频文件及附加文件"); }
}
