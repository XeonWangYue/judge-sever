package top.xeonwang.JudgeServer.exception;

/**
 * 认证/Token 专用业务异常
 */
public class AuthException extends RuntimeException {
    // 自定义错误码
    private final Integer code;
    private final String msg;

    public AuthException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
