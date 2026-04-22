package com.ueascend.JudgeServer.entity.auth;

import lombok.Data;

/***
 * 组信息，每个用户/每个赛事都可能会有对应的分组
 */
@Data
public class Group {
    // 组Id
    private Long groupId;
    // 组名称
    private String groupName;
}
