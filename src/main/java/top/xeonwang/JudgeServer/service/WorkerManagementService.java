package top.xeonwang.JudgeServer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.xeonwang.JudgeServer.entity.worker.WorkerDeployRequest;
import top.xeonwang.JudgeServer.utils.SshUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Worker 管理服务
 * <p>
 * 负责将编译好的 Go Worker 可执行文件（goworker-<平台>-<版本号>）
 * 通过 SSH/SFTP 上传到目标服务器并远程启动。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkerManagementService {

    private final SshUtil sshUtil;

    /**
     * 存放编译产物的本地目录，可在 application.yml 中通过 worker.source-dir 配置
     */
    @Value("${worker.source-dir:./source}")
    private String sourceDir;

    /**
     * 部署并启动 Worker
     *
     * @param request 部署请求参数
     * @return 命令执行输出
     */
    public String deployAndStart(WorkerDeployRequest request) throws Exception {
        // 1. 定位本地可执行文件
        String binaryName = buildBinaryName(request.getPlatform(), request.getVersion());
        Path localPath = Paths.get(sourceDir, binaryName);

        if (!Files.exists(localPath)) {
            throw new IllegalStateException(
                    "本地可执行文件不存在: " + localPath.toAbsolutePath());
        }

        // 2. 确定远程路径
        String remoteDir = request.getRemoteDir();
        if (remoteDir == null || remoteDir.isBlank()) {
            remoteDir = "/opt/goworker";
        }
        // 确保远程目录以 / 结尾
        if (!remoteDir.endsWith("/")) {
            remoteDir = remoteDir + "/";
        }
        String remotePath = remoteDir + binaryName;

        String host = request.getHost();
        String username = request.getUsername();
        String password = request.getPassword();

        // 3. 先创建远程目录
        log.info("在目标服务器 {} 上创建目录 {}", host, remoteDir);
        sshUtil.execCommand(host, username, password, "mkdir -p " + remoteDir);

        // 4. 通过 SFTP 上传文件
        log.info("开始上传文件 {} → {}:{} ", localPath, host, remotePath);
        sshUtil.uploadFile(host, username, password,
                localPath.toAbsolutePath().toString(), remotePath);

        // 5. 赋予可执行权限
        log.info("赋予远程文件可执行权限: chmod +x {}", remotePath);
        sshUtil.execCommand(host, username, password, "chmod +x " + remotePath);

        // 6. 组装启动命令并执行
        String startCommand = buildStartCommand(remotePath, request.getExtraArgs());
        log.info("在目标服务器 {} 上执行启动命令: {}", host, startCommand);
        String output = sshUtil.execCommand(host, username, password, startCommand);

        log.info("Worker 启动命令执行完毕，输出:\n{}", output);
        return output;
    }

    /**
     * 仅上传文件（不启动）
     */
    public void upload(WorkerDeployRequest request) throws Exception {
        String binaryName = buildBinaryName(request.getPlatform(), request.getVersion());
        Path localPath = Paths.get(sourceDir, binaryName);

        if (!Files.exists(localPath)) {
            throw new IllegalStateException(
                    "本地可执行文件不存在: " + localPath.toAbsolutePath());
        }

        String remoteDir = request.getRemoteDir();
        if (remoteDir == null || remoteDir.isBlank()) {
            remoteDir = "/opt/goworker";
        }
        if (!remoteDir.endsWith("/")) {
            remoteDir = remoteDir + "/";
        }
        String remotePath = remoteDir + binaryName;

        String host = request.getHost();
        String username = request.getUsername();
        String password = request.getPassword();

        // 创建远程目录
        sshUtil.execCommand(host, username, password, "mkdir -p " + remoteDir);

        // 上传文件
        log.info("上传文件 {} → {}:{} ", localPath, host, remotePath);
        sshUtil.uploadFile(host, username, password,
                localPath.toAbsolutePath().toString(), remotePath);

        // 赋予可执行权限
        sshUtil.execCommand(host, username, password, "chmod +x " + remotePath);
        log.info("文件上传完成: {}:{} ", host, remotePath);
    }

    /**
     * 在远程服务器上启动已部署的 Worker
     */
    public String start(WorkerDeployRequest request) throws Exception {
        String binaryName = buildBinaryName(request.getPlatform(), request.getVersion());

        String remoteDir = request.getRemoteDir();
        if (remoteDir == null || remoteDir.isBlank()) {
            remoteDir = "/opt/goworker";
        }
        if (!remoteDir.endsWith("/")) {
            remoteDir = remoteDir + "/";
        }
        String remotePath = remoteDir + binaryName;

        String startCommand = buildStartCommand(remotePath, request.getExtraArgs());
        log.info("在目标服务器 {} 上执行启动命令: {}", request.getHost(), startCommand);

        String output = sshUtil.execCommand(
                request.getHost(), request.getUsername(), request.getPassword(), startCommand);
        log.info("Worker 启动命令执行完毕，输出:\n{}", output);
        return output;
    }

    /**
     * 在远程服务器上停止 Worker 进程
     */
    public String stop(WorkerDeployRequest request) throws Exception {
        String binaryName = buildBinaryName(request.getPlatform(), request.getVersion());
        String remoteDir = request.getRemoteDir();
        if (remoteDir == null || remoteDir.isBlank()) {
            remoteDir = "/opt/goworker";
        }
        if (!remoteDir.endsWith("/")) {
            remoteDir = remoteDir + "/";
        }
        String remotePath = remoteDir + binaryName;

        String stopCommand = "pkill -f " + remotePath;
        log.info("在目标服务器 {} 上执行停止命令: {}", request.getHost(), stopCommand);

        String output = sshUtil.execCommand(
                request.getHost(), request.getUsername(), request.getPassword(), stopCommand);
        log.info("Worker 停止命令执行完毕，输出:\n{}", output);
        return output;
    }

    /**
     * 列出 source 目录下所有可用的 Worker 二进制文件
     */
    public List<String> listAvailableBinaries() {
        List<String> result = new ArrayList<>();
        File dir = Paths.get(sourceDir).toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            return result;
        }
        File[] files = dir.listFiles((d, name) -> name.startsWith("goworker-"));
        if (files != null) {
            for (File f : files) {
                result.add(f.getName());
            }
        }
        return result;
    }

    /**
     * 构建二进制文件名：goworker-<platform>-<version>
     */
    private String buildBinaryName(String platform, String version) {
        return "goworker-" + platform + "-" + version;
    }

    /**
     * 构建远程启动命令
     * <p>
     * 使用 nohup + & 使进程在后台运行，避免 SSH 断开后进程被杀
     */
    private String buildStartCommand(String remotePath, String extraArgs) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("nohup ").append(remotePath);
        if (extraArgs != null && !extraArgs.isBlank()) {
            cmd.append(" ").append(extraArgs);
        }
        cmd.append(" > /dev/null 2>&1 &");
        return cmd.toString();
    }
}