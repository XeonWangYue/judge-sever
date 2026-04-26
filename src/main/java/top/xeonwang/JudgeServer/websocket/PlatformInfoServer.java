package top.xeonwang.JudgeServer.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import top.xeonwang.JudgeServer.component.WebSocketSessionManager;
import top.xeonwang.JudgeServer.entity.auth.RedisPrefixConstants;
import top.xeonwang.JudgeServer.entity.ws.WsUser;
import top.xeonwang.JudgeServer.utils.JsonUtil;
import top.xeonwang.JudgeServer.utils.JwtUtil;
import top.xeonwang.JudgeServer.utils.RedisUtil;
import top.xeonwang.JudgeServer.utils.SpringContextUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Component
@ServerEndpoint("/ws/info")
@Slf4j
public class PlatformInfoServer {

    private final WebSocketSessionManager webSocketSessionManager = SpringContextUtil.getBean(WebSocketSessionManager.class);

    private final RedisUtil redisUtil = SpringContextUtil.getBean(RedisUtil.class);

    private final JwtUtil jwtUtil = SpringContextUtil.getBean(JwtUtil.class);

    private Session session;
    private String userId;
    private String closeReason;

    // TODO 想一个好办法，拒绝重复连接，服务器重启时又不能
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        try {
            try {
                String token = this.session.getRequestParameterMap().get("token").get(0);
                String userId = jwtUtil.getUserIdFromToken(token);
                log.info("WebSocket 连接成功，用户ID：{}", userId);
                this.userId = userId;
            } catch (Exception e) {
                log.error("验证token失败", e);
                this.closeReason = "鉴权失败";
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, this.closeReason));
                return;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }

        String redisUserKey = RedisPrefixConstants.WS_USER_KEY + this.userId;
        if (redisUtil.get(redisUserKey) != null && webSocketSessionManager.hasSession(this.userId)) {
            try {
                this.closeReason = "重复连接";
                log.warn("重复连接，直接断开");
                session.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            return;
        }
        redisUtil.set(
                redisUserKey,
                JsonUtil.toJsonString(new WsUser(this.userId, this.session.getId()))
        );
        webSocketSessionManager.addSession(this.userId, this.session);

        log.info("WebSocket 连接成功：userId={}", userId);
    }

    @OnClose
    public void onClose(Session session) {
        // 从 Redis 移除
        if (this.closeReason != null && this.closeReason.equals("重复连接")) {
            // 不做任何事，防止挤掉
            log.warn("断开但不清理");
        } else {
            redisUtil.del(RedisPrefixConstants.WS_USER_KEY + userId);
            webSocketSessionManager.removeSession(this.userId);
        }

        log.info("WebSocket 断开连接：userId={}", userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {

    }

    @OnError
    public void onError(Session session, Throwable error) {

    }

}
