package com.rally.web.config;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.AuthException;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tennis.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器
 * 统一处理业务异常，返回标准错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthException.class)
    public Result<Void> handleAuthException(AuthException e) {
        log.warn("认证异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.fail(e.getErrorCode());
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.fail(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", msg);
        return Result.fail(BizErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getMessage());
        return Result.fail(BizErrorCode.PARAM_ERROR.getCode(), "参数类型错误");
    }

    /**
     * 处理未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(BizErrorCode.OPERATION_FAILED.getCode(), "系统异常，请稍后重试");
    }
}
