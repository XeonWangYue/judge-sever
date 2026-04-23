package top.xeonwang.JudgeServer.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.xeonwang.JudgeServer.entity.auth.TokenConstants;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${token.secret}")
    private String secret;

    @Value("${token.access-expire}")
    private long accessExpire;

    @Value("${token.refresh-expire}")
    private long refreshExpire;

    /**
     * 获取密钥
     */
    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成短期 AccessToken
     */
    public String createAccessToken(String userId, Map<String, Object> claims) {
        return createToken(userId, claims, accessExpire, TokenConstants.ACCESS_TOKEN);
    }

    /**
     * 生成长期 RefreshToken
     */
    public String createRefreshToken(String userId) {
        return createToken(userId, null, refreshExpire, TokenConstants.REFRESH_TOKEN);
    }

    /**
     * 统一生成Token
     */
    private String createToken(String userId, Map<String, Object> claims, long expireTime, String tokenType) {
        if (claims == null || claims.isEmpty()) {
            claims = new HashMap<>();
        }
        return Jwts.builder()
                .setClaims(claims)  // 自定义信息（权限、用户信息等）
                .setSubject(userId) // 用户ID（唯一标识）
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expireTime)) // 过期时间
                .claim("token_type", tokenType) // 区分长短期Token
                .signWith(getSignKey(), SignatureAlgorithm.HS256) // 签名算法
                .compact();
    }

    /**
     * 解析Token获取用户ID
     */
    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 获取Token类型（access/refresh）
     */
    public String getTokenType(String token) {
        return (String) getClaimsFromToken(token).get("token_type");
    }

    /**
     * 解析Token
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 校验Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
