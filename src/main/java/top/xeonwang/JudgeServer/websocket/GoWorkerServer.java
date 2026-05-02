package top.xeonwang.JudgeServer.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.xeonwang.JudgeServer.component.WorkerSessionManager;
import top.xeonwang.JudgeServer.entity.worker.Report;
import top.xeonwang.JudgeServer.entity.worker.WorkerMsg;
import top.xeonwang.JudgeServer.service.GoWorkerService;
import top.xeonwang.JudgeServer.utils.JsonUtil;
import top.xeonwang.JudgeServer.utils.SpringContextUtil;

import java.io.IOException;
import java.util.UUID;


@Component
@ServerEndpoint("/ws/jserver")
@Slf4j
public class GoWorkerServer {

    private final GoWorkerService goWorkerService = SpringContextUtil.getBean(GoWorkerService.class);

    private final WorkerSessionManager workerSessionManager = SpringContextUtil.getBean(WorkerSessionManager.class);

    private Session session;
    private String workerId;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        // 为每个Worker生成唯一ID
        this.workerId = UUID.randomUUID().toString();
        workerSessionManager.addSession(this.workerId, this.session);
        log.info("Worker WebSocket 连接成功: workerId={}", this.workerId);
    }

    @OnClose
    public void onClose(Session session) {
        if (this.workerId != null) {
            workerSessionManager.removeSession(this.workerId);
        }
        log.info("Worker WebSocket 断开连接: workerId={}", this.workerId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            var data = JsonUtil.toObject(message, WorkerMsg.class);

            switch (data.getMsgType()) {
                case "monitor":
                    var msg = JsonUtil.convertToTargetObject(data.getData(), Report.class);
                    goWorkerService.saveMonitorData(msg, "1");
                    log.info("WebSocket 收到Worker系统信息: {}", msg);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {

    }
}
