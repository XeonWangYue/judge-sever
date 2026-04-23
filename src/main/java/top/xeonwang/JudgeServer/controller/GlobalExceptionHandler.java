package top.xeonwang.JudgeServer.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.xeonwang.JudgeServer.common.ResponseBody;
import top.xeonwang.JudgeServer.exception.AuthException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 专门捕获 认证/Token 异常
    @ExceptionHandler(AuthException.class)
    public ResponseBody authExceptionHandler(AuthException e) {
        return new ResponseBody(401, e.getMsg(), null);
    }

    // 兜底：全局未知异常
    @ExceptionHandler(Exception.class)
    public ResponseBody exceptionHandler(Exception e) {
        log.error(e.getMessage(), e);
        return new ResponseBody(500, "服务器内部错误", null);
    }
}