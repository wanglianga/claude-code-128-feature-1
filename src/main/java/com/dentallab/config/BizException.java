package com.dentallab.config;

import org.springframework.http.HttpStatus;

/** 业务异常，携带 HTTP 状态码。 */
public class BizException extends RuntimeException {

    private final HttpStatus status;

    public BizException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BizException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BizException notFound(String what) {
        return new BizException(HttpStatus.NOT_FOUND, what + "不存在");
    }

    public static BizException forbidden(String message) {
        return new BizException(HttpStatus.FORBIDDEN, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(HttpStatus.UNAUTHORIZED, message);
    }
}
