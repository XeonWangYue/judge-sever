package top.xeonwang.JudgeServer.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.xeonwang.JudgeServer.common.ResponseBody;
import top.xeonwang.JudgeServer.service.AuthService;
import top.xeonwang.JudgeServer.utils.CookieUtil;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @Resource
    private CookieUtil cookieUtil;

    /**
     * 使用初始令牌登录，获取长短期Token
     */
    @PostMapping("/login")
    public ResponseBody login(@RequestParam String initToken) {
        return new ResponseBody(authService.loginByInitToken(initToken));
    }

    /**
     * 刷新AccessToken
     */
    @PostMapping("/refresh")
    public ResponseBody refresh(HttpServletRequest request) {
        return new ResponseBody(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseBody logout(HttpServletRequest request) {
        // 1. 从cookie拿refreshToken，删除redis记录
        // 2. 清空客户端cookie
        cookieUtil.clearRefreshTokenCookie();
        return new ResponseBody();
    }
}
