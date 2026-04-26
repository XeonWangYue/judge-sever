package top.xeonwang.JudgeServer.exception;

import lombok.Getter;

/**
 * 认证/Token 专用业务异常
 */
@Getter
public class AuthException extends RuntimeException {
    // 自定义错误码
    private final Integer code;
    private final String msg;

    public AuthException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

}
