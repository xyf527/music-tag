package com.xin.musictag.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.Map;

@RestControllerAdvice
public final class UserErrorHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Map<String,String> tooLarge() { return Map.of("errorCode", "FILE_TOO_LARGE", "message", "文件超过大小限制，请选择较小的音频文件"); }
    @ExceptionHandler({IllegalArgumentException.class, java.io.IOException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalid(Exception ignored) { return Map.of("errorCode", "INVALID_UPLOAD", "message", "文件无效或不符合安全要求"); }
}
