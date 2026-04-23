package top.xeonwang.JudgeServer.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.xeonwang.JudgeServer.entity.auth.TokenConstants;
import top.xeonwang.JudgeServer.utils.JwtUtil;


@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头获取Token
        String accessToken = request.getHeader("Authorization");
        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            throw new RuntimeException("请携带有效的访问令牌");
        }

        // 2. 截取Token
        accessToken = accessToken.replace("Bearer ", "");

        // 3. 校验Token
        if (!jwtUtil.validateToken(accessToken)) {
            throw new RuntimeException("访问令牌已过期或无效");
        }

        // 4. 校验Token类型必须是AccessToken
        String tokenType = jwtUtil.getTokenType(accessToken);
        if (!TokenConstants.ACCESS_TOKEN.equals(tokenType)) {
            throw new RuntimeException("请使用访问令牌");
        }

        // 5. 把用户信息存入request，供接口使用
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        request.setAttribute("userId", userId);
        return true;
    }
}
