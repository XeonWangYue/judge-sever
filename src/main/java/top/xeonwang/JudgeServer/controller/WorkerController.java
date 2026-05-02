package top.xeonwang.JudgeServer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.xeonwang.JudgeServer.common.ResultVO;
import top.xeonwang.JudgeServer.entity.worker.WorkerDeployRequest;
import top.xeonwang.JudgeServer.service.WorkerManagementService;

import java.util.List;

/**
 * Worker 管理接口
 */
@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerManagementService workerManagementService;

    /**
     * 部署并启动 Worker（上传可执行文件 + 远程启动）
     */
    @PostMapping("/deploy")
    public ResultVO<String> deploy(@RequestBody WorkerDeployRequest request) {
        try {
            String output = workerManagementService.deployAndStart(request);
            return new ResultVO<>(output);
        } catch (Exception e) {
            return new ResultVO<>(500, "部署失败: " + e.getMessage());
        }
    }

    /**
     * 仅上传 Worker 可执行文件到远程服务器（不启动）
     */
    @PostMapping("/upload")
    public ResultVO<Void> upload(@RequestBody WorkerDeployRequest request) {
        try {
            workerManagementService.upload(request);
            return new ResultVO<>();
        } catch (Exception e) {
            return new ResultVO<>(500, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 启动远程服务器上已部署的 Worker
     */
    @PostMapping("/start")
    public ResultVO<String> start(@RequestBody WorkerDeployRequest request) {
        try {
            String output = workerManagementService.start(request);
            return new ResultVO<>(output);
        } catch (Exception e) {
            return new ResultVO<>(500, "启动失败: " + e.getMessage());
        }
    }

    /**
     * 停止远程服务器上的 Worker 进程
     */
    @PostMapping("/stop")
    public ResultVO<String> stop(@RequestBody WorkerDeployRequest request) {
        try {
            String output = workerManagementService.stop(request);
            return new ResultVO<>(output);
        } catch (Exception e) {
            return new ResultVO<>(500, "停止失败: " + e.getMessage());
        }
    }

    /**
     * 列出本地 source 目录下所有可用的 Worker 二进制文件
     */
    @GetMapping("/binaries")
    public ResultVO<List<String>> listBinaries() {
        List<String> binaries = workerManagementService.listAvailableBinaries();
        return new ResultVO<>(binaries);
    }
}
