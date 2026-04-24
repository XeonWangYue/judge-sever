package top.xeonwang.JudgeServer.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class CookieUtil {

    /**
     * 写入 RefreshToken 到 Cookie
     *
     * @param refreshToken 长期刷新令牌
     * @param maxAgeSecond 过期秒数
     */
    public void setRefreshTokenCookie(String refreshToken, long maxAgeSecond) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletResponse response = attributes.getResponse();
        if (response == null) {
            return;
        }

        // 构建安全 Cookie
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)       // 关键：JS 无法读取，防XSS
                .secure(false)        // 本地/内网没HTTPS先关，线上HTTPS改为true
                .path("/")            // 作用路径
                .maxAge(maxAgeSecond) // 过期时间 秒
                .sameSite("Lax")      // 防CSRF
                .build();

        // 设置到响应头
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 清除 RefreshToken Cookie（登出使用）
     */
    public void clearRefreshTokenCookie() {
        setRefreshTokenCookie("", 0);
    }
}
