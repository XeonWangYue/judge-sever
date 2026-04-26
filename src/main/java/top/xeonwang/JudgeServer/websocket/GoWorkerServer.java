package top.xeonwang.JudgeServer.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.xeonwang.JudgeServer.entity.worker.Report;
import top.xeonwang.JudgeServer.entity.worker.WorkerMsg;
import top.xeonwang.JudgeServer.service.GoWorkerService;
import top.xeonwang.JudgeServer.utils.JsonUtil;
import top.xeonwang.JudgeServer.utils.SpringContextUtil;


@Component
@ServerEndpoint("/ws/jserver")
@Slf4j
public class GoWorkerServer {

    private final GoWorkerService goWorkerService = SpringContextUtil.getBean(GoWorkerService.class);

    @OnOpen
    public void onOpen(Session session) {

        log.info("WebSocket 连接成功");
    }

    @OnClose
    public void onClose(Session session) {
        log.info("WebSocket 断开连接");
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
