package org.example.collaborative_editor.exception;

import lombok.Getter;

/**
 * 业务异常
 * 用于在业务逻辑中抛出可预期的异常
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}

