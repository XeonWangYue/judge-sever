package top.xeonwang.JudgeServer.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.xeonwang.JudgeServer.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    /**
     * 使用初始令牌登录，获取长短期Token
     */
    @PostMapping("/login")
    public Map<String, String> login(@RequestParam String initToken) {
        return authService.loginByInitToken(initToken);
    }

    /**
     * 刷新AccessToken
     */
    @PostMapping("/refresh")
    public Map<String, String> refresh(@RequestParam String refreshToken) {
        return authService.refreshToken(refreshToken);
    }
}
