package top.xeonwang.JudgeServer.component;

import jakarta.websocket.Session;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final Map<String, Session> SESSION_POOL = new ConcurrentHashMap<>();

    public boolean hasSession(String userId) {
        return SESSION_POOL.containsKey(userId);
    }

    public Session getSession(String userId) {
        return SESSION_POOL.get(userId);
    }

    public void addSession(String userId, Session session) {
        SESSION_POOL.put(userId, session);

    }

    public void removeSession(String userId) {
        SESSION_POOL.remove(userId);
    }
}
