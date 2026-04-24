package top.xeonwang.JudgeServer.service;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.xeonwang.JudgeServer.entity.auth.RedisPrefixConstants;
import top.xeonwang.JudgeServer.entity.auth.TokenConstants;
import top.xeonwang.JudgeServer.exception.AuthCode;
import top.xeonwang.JudgeServer.exception.AuthException;
import top.xeonwang.JudgeServer.utils.CookieUtil;
import top.xeonwang.JudgeServer.utils.JwtUtil;
import top.xeonwang.JudgeServer.utils.RedisUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private CookieUtil cookieUtil;

    /**
     * 初始令牌校验，返回长短期Token
     */
    public Map<String, String> loginByInitToken(String initToken) {
        String redisKey = RedisPrefixConstants.INIT_TOKEN_KEY + initToken;
        // 1. 校验初始令牌是否存在
        if (!redisUtil.hasKey(redisKey)) {
            throw new AuthException(AuthCode.INIT_TOKEN_ERR, "初始令牌无效或已过期");
        }

        // 2. 获取用户ID
        String userId = (String) redisUtil.get(redisKey);

        // 4. 生成自定义信息（权限、用户信息等）
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", "admin"); // 可存储角色权限

        // 5. 生成长短期Token
        String accessToken = jwtUtil.createAccessToken(userId, claims);
        String refreshToken = jwtUtil.createRefreshToken(userId);

        // 6. 存储RefreshToken到Redis（支持过期/强制下线）
        String refreshRedisKey = RedisPrefixConstants.REFRESH_TOKEN_KEY + userId;
        redisUtil.set(refreshRedisKey, refreshToken, 7, TimeUnit.DAYS);

        cookieUtil.setRefreshTokenCookie(refreshToken, 7 * 86400);

        // 7. 返回给前端
        Map<String, String> result = new HashMap<>();
        result.put("accessToken", accessToken);
        return result;
    }

    /**
     * 刷新AccessToken（前端传入RefreshToken）
     */
    public Map<String, String> refreshToken(HttpServletRequest request) {
        String refreshToken = null;
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        // 2. 判空抛异常
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthCode.REFRESH_TOKEN_ERR, "未携带刷新令牌，请重新登录");
        }

        // 1. 校验RefreshToken格式和签名
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new AuthException(AuthCode.REFRESH_TOKEN_ERR, "刷新令牌无效");
        }

        // 2. 必须是RefreshToken类型
        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!TokenConstants.REFRESH_TOKEN.equals(tokenType)) {
            throw new AuthException(AuthCode.TOKEN_TYPE_ERR, "请使用刷新令牌");
        }

        // 3. 获取用户ID
        String userId = jwtUtil.getUserIdFromToken(refreshToken);
        String refreshRedisKey = RedisPrefixConstants.REFRESH_TOKEN_KEY + userId;

        // 4. 校验Redis中的RefreshToken一致
        String redisRefreshToken = (String) redisUtil.get(refreshRedisKey);
        if (!refreshToken.equals(redisRefreshToken)) {
            // 不一致说明令牌已被顶下线/强制注销
            throw new AuthException(AuthCode.TOKEN_EXPIRE, "刷新令牌已失效，请重新登录");
        }

        // 5. 生成新的AccessToken
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        String newAccessToken = jwtUtil.createAccessToken(userId, claims);

        // 6. 返回新的短期Token
        Map<String, String> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        return result;
    }
}
