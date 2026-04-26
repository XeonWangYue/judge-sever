package top.xeonwang.JudgeServer.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.xeonwang.JudgeServer.common.ResultVO;
import top.xeonwang.JudgeServer.exception.AuthException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 专门捕获 认证/Token 异常
    @ExceptionHandler(AuthException.class)
    public ResultVO authExceptionHandler(AuthException e) {
        return new ResultVO(401, e.getMsg(), null);
    }

    // 兜底：全局未知异常
    @ExceptionHandler(Exception.class)
    public ResultVO exceptionHandler(Exception e) {
        log.error(e.getMessage(), e);
        return new ResultVO(500, "服务器内部错误", null);
    }
}