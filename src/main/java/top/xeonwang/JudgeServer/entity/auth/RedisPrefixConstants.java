package top.xeonwang.JudgeServer.entity.auth;

public class RedisPrefixConstants {
    /**
     * Redis
     */
    public static final String WS_USER_KEY = "ws:user:";

    /**
     * Redis 存储初始登录令牌前缀
     */
    public static final String INIT_TOKEN_KEY = "init:token:";

    /**
     * Redis 存储刷新令牌前缀
     */
    public static final String REFRESH_TOKEN_KEY = "refresh:token:";

    public static final String LOCK_KEY_PREFIX = "lock:user:";
}
