package top.xeonwang.JudgeServer.exception;

public class AuthCode {
    // token相关错误码
    public static final int TOKEN_EMPTY = 40001;       // token为空
    public static final int TOKEN_INVALID = 40002;     // token格式错误/签名错误
    public static final int TOKEN_EXPIRE = 40003;      // token已过期
    public static final int TOKEN_TYPE_ERR = 40004;    // token类型错误
    public static final int REFRESH_TOKEN_ERR = 40005; // 刷新令牌失效
    public static final int INIT_TOKEN_ERR = 40006;    // 初始登录令牌无效
}