package top.xeonwang.JudgeServer.entity.worker;

import lombok.Data;

/**
 * Worker 部署请求
 */
@Data
public class WorkerDeployRequest {
    /**
     * 目标平台，如 linux-amd64, linux-arm64, darwin-amd64 等
     */
    private String platform;

    /**
     * 版本号
     */
    private String version;

    /**
     * 目标服务器 IP / 主机名
     */
    private String host;

    /**
     * SSH 用户名
     */
    private String username;

    /**
     * SSH 密码
     */
    private String password;

    /**
     * 远程服务器上存放可执行文件的路径（目录）
     */
    private String remoteDir;

    /**
     * 启动命令的额外参数（可选）
     */
    private String extraArgs;
}