package top.xeonwang.JudgeServer.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@ServerEndpoint("/ws/jserver")
@Slf4j
public class GoWorkerServer {
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
        log.info("WebSocket 收到信息: {}", message);
    }

    @OnError
    public void onError(Session session, Throwable error) {

    }
}
