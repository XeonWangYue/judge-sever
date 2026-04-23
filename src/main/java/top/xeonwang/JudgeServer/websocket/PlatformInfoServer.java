package top.xeonwang.JudgeServer.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.xeonwang.JudgeServer.entity.auth.RedisPrefixConstants;
import top.xeonwang.JudgeServer.entity.ws.WsUser;
import top.xeonwang.JudgeServer.utils.RedisUtil;
import top.xeonwang.JudgeServer.utils.SpringContextUtil;

import java.util.concurrent.TimeUnit;


@Component
@ServerEndpoint("/ws/info")
@Slf4j
public class PlatformInfoServer {

    // 连接过期时间（心跳超时时间）
    private static final long EXPIRE_MINUTES = 5;

    private final RedisUtil redisUtil = SpringContextUtil.getBean(RedisUtil.class);

    private Session session;
    private String userId;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.userId = userId;

        redisUtil.set(RedisPrefixConstants.WS_USER_KEY + userId, (Object) new WsUser(), EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("WebSocket 连接成功：userId={}", userId);
    }

    @OnClose
    public void onClose(Session session) {
        // 从 Redis 移除
        redisUtil.del(RedisPrefixConstants.WS_USER_KEY + userId);
        log.info("WebSocket 断开连接：userId={}", userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {

    }

    @OnError
    public void onError(Session session, Throwable error) {

    }

}
