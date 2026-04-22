package com.ueascend.JudgeServer.entity.auth;

import lombok.Data;

import java.util.List;

/**
 * 基本用户信息
 */
@Data
public class User {
    private Long userId;
    private String nickName;
    private String avatarUrl;

    // 真实信息可以考虑不放在这里
    private String realName;
    private String employeeId;
    // 多层部门逐层显示
    private List<String> departments;
}
