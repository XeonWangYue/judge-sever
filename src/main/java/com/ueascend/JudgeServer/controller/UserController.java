/*
    用户管理
 */
package com.ueascend.JudgeServer.controller;

import com.ueascend.JudgeServer.common.ResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 这里最好用标准的
 */
@RequestMapping("/user")
@RestController
@Slf4j
public class UserController {

    /**
     * 用户登录
     *
     * @param token token信息
     * @return rb
     */
    @PostMapping("/login")
    public ResponseBody<String> loginUser(@RequestBody String token) {
        //TODO 令牌校验+长token保持
        log.info("Current Token: {}", token);
        return new ResponseBody<>();
    }
}
