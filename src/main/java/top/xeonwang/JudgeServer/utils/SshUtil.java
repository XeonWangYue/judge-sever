package top.xeonwang.JudgeServer.utils;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
@Slf4j
public class SshUtil {
    private static final int SSH_PORT = 22;
    private static final int TIMEOUT = 30000;

    /**
     * 上传文件到远程服务器
     */
    public void uploadFile(String ip, String username, String password,
                           String localPath, String remotePath) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, ip, SSH_PORT);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(TIMEOUT);

        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();

        try (FileInputStream fis = new FileInputStream(localPath)) {
            sftp.put(fis, remotePath);
            log.info("文件上传成功：{} → {}", localPath, remotePath);
        } finally {
            sftp.disconnect();
            session.disconnect();
        }
    }

    /**
     * 远程执行 Shell 命令（启动 Worker）
     */
    public String execCommand(String ip, String username, String password, String command) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, ip, SSH_PORT);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(TIMEOUT);

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setErrStream(System.err);
        channel.connect();

        // 读取执行结果
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }

        channel.disconnect();
        session.disconnect();
        return result.toString();
    }
}