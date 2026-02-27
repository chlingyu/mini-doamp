package com.demo.minidoamp.core.config;

import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return R.fail(400, msg);
    }

    @ExceptionHandler(BindException.class)
    public R<?> handleBind(BindException e) {
        String msg = e.getAllErrors().get(0).getDefaultMessage();
        return R.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail("系统内部错误");
    }
}
