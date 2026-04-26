package top.xeonwang.JudgeServer.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.xeonwang.JudgeServer.common.ResultVO;
import top.xeonwang.JudgeServer.service.AuthService;
import top.xeonwang.JudgeServer.utils.CookieUtil;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final CookieUtil cookieUtil;

    /**
     * 使用初始令牌登录，获取长短期Token
     */
    @PostMapping("/login")
    public ResultVO login(@RequestParam String initToken) {
        return new ResultVO(authService.loginByInitToken(initToken));
    }

    /**
     * 刷新AccessToken
     */
    @PostMapping("/refresh")
    public ResultVO refresh(HttpServletRequest request) {
        return new ResultVO(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResultVO logout(HttpServletRequest request) {
        // 1. 从cookie拿refreshToken，删除redis记录
        // 2. 清空客户端cookie
        cookieUtil.clearRefreshTokenCookie();
        return new ResultVO();
    }
}
