package top.xeonwang.JudgeServer.component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.xeonwang.JudgeServer.entity.ws.PooledSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * 用户WebSocket连接池管理器
 * <p>
 * 在启动时预初始化一批空闲槽位，用户连接时从池中分配，断开时回收至池中。
 * 提供群发消息和点对点发送消息的功能。
 */
@Component
@Slf4j
public class UserSessionManager {

    /**
     * 连接池总容量（可通过 user.pool.size 配置）
     */
    @Value("${user.pool.size:64}")
    private int poolSize;

    /**
     * 连接池: slotId -> PooledSession
     */
    private final Map<String, PooledSession> sessionPool = new ConcurrentHashMap<>();

    /**
     * 业务ID到槽位ID的反向映射: userId -> slotId
     */
    private final Map<String, String> userSlotMap = new ConcurrentHashMap<>();

    /**
     * 空闲槽位队列（线程安全 FIFO）
     */
    private final ConcurrentLinkedQueue<String> idleQueue = new ConcurrentLinkedQueue<>();

    /**
     * 健康检查调度器
     */
    private ScheduledExecutorService healthCheckExecutor;

    @PostConstruct
    public void init() {
        // 启动时预初始化连接池
        for (int i = 0; i < poolSize; i++) {
            String slotId = "user-slot-" + i;
            sessionPool.put(slotId, new PooledSession(slotId));
            idleQueue.offer(slotId);
        }
        log.info("用户连接池初始化完成，总容量: {}", poolSize);

        // 启动定期健康检查（每30秒）
        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "user-pool-health-check");
            t.setDaemon(true);
            return t;
        });
        healthCheckExecutor.scheduleAtFixedRate(this::healthCheck, 30, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        // 关闭所有活跃连接
        for (PooledSession pooled : sessionPool.values()) {
            if (pooled.isActive() && pooled.getSession() != null && pooled.getSession().isOpen()) {
                try {
                    pooled.getSession().close();
                } catch (IOException e) {
                    log.warn("用户连接池 - 关闭Session失败: slotId={}", pooled.getSlotId(), e);
                }
            }
            pooled.markClosed();
        }
        sessionPool.clear();
        userSlotMap.clear();
        idleQueue.clear();

        if (healthCheckExecutor != null) {
            healthCheckExecutor.shutdown();
        }
        log.info("用户连接池已销毁");
    }

    /**
     * 从池中分配一个空闲槽位给用户（IDLE → ACTIVE）
     *
     * @param userId  用户ID
     * @param session WebSocket Session
     * @return 分配到的槽位ID，无可用槽位返回null
     */
    public String acquireSlot(String userId, Session session) {
        // 先检查该用户是否已经占用槽位
        if (userSlotMap.containsKey(userId)) {
            log.warn("用户连接池 - 用户已占用槽位: userId={}", userId);
            return null;
        }

        // 从空闲队列取一个槽位
        String slotId = idleQueue.poll();
        if (slotId == null) {
            log.warn("用户连接池 - 无可用槽位，拒绝连接: userId={}, 当前活跃数={}", userId, getActiveCount());
            return null;
        }

        PooledSession pooled = sessionPool.get(slotId);
        if (pooled == null || !pooled.bind(userId, session)) {
            // 绑定失败，归还槽位
            idleQueue.offer(slotId);
            log.warn("用户连接池 - 槽位绑定失败: slotId={}, userId={}", slotId, userId);
            return null;
        }

        userSlotMap.put(userId, slotId);
        log.info("用户连接池 - 分配槽位: userId={}, slotId={}, 当前活跃用户数: {}, 空闲槽位数: {}",
                userId, slotId, getActiveCount(), getIdleCount());
        return slotId;
    }

    /**
     * 释放用户占用的槽位（ACTIVE → IDLE）
     *
     * @param userId 用户ID
     */
    public void releaseSlot(String userId) {
        String slotId = userSlotMap.remove(userId);
        if (slotId == null) {
            log.debug("用户连接池 - 用户未占用槽位: userId={}", userId);
            return;
        }

        PooledSession pooled = sessionPool.get(slotId);
        if (pooled != null && pooled.release()) {
            idleQueue.offer(slotId);
            log.info("用户连接池 - 回收槽位: userId={}, slotId={}, 当前活跃用户数: {}, 空闲槽位数: {}",
                    userId, slotId, getActiveCount(), getIdleCount());
        }
    }

    // ========== 兼容旧接口 ==========

    /**
     * 添加用户连接（等同于 acquireSlot）
     *
     * @param userId  用户ID
     * @param session WebSocket Session
     */
    public void addSession(String userId, Session session) {
        acquireSlot(userId, session);
    }

    /**
     * 移除用户连接（等同于 releaseSlot）
     *
     * @param userId 用户ID
     */
    public void removeSession(String userId) {
        releaseSlot(userId);
    }

    // ========== 查询接口 ==========

    /**
     * 判断用户是否已有连接
     *
     * @param userId 用户ID
     * @return 是否存在活跃连接
     */
    public boolean hasSession(String userId) {
        return userSlotMap.containsKey(userId);
    }

    /**
     * 获取用户的Session
     *
     * @param userId 用户ID
     * @return Session对象，不存在或空闲则返回null
     */
    public Session getSession(String userId) {
        String slotId = userSlotMap.get(userId);
        if (slotId == null) {
            return null;
        }
        PooledSession pooled = sessionPool.get(slotId);
        return pooled != null ? pooled.getSession() : null;
    }

    /**
     * 获取用户对应的PooledSession（包含完整池化信息）
     *
     * @param userId 用户ID
     * @return PooledSession对象
     */
    public PooledSession getPooledSession(String userId) {
        String slotId = userSlotMap.get(userId);
        if (slotId == null) {
            return null;
        }
        return sessionPool.get(slotId);
    }

    /**
     * 获取当前活跃用户数量
     */
    public int getActiveCount() {
        return userSlotMap.size();
    }

    /**
     * 获取当前在线用户数（与 getActiveCount 相同，兼容旧接口）
     */
    public int getOnlineCount() {
        return getActiveCount();
    }

    /**
     * 获取空闲槽位数量
     */
    public int getIdleCount() {
        return poolSize - getActiveCount();
    }

    /**
     * 获取连接池总容量
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * 获取所有在线用户ID列表
     */
    public Collection<String> getAllUserIds() {
        return Collections.unmodifiableSet(userSlotMap.keySet());
    }

    /**
     * 获取连接池中所有槽位的快照（用于监控/调试）
     */
    public Collection<PooledSession> getAllPooledSessions() {
        return Collections.unmodifiableCollection(sessionPool.values());
    }

    // ========== 消息发送接口 ==========

    /**
     * 群发消息给所有在线用户
     *
     * @param message 消息内容
     */
    public void broadcastToAll(String message) {
        int successCount = 0;
        int failCount = 0;
        for (Map.Entry<String, String> entry : userSlotMap.entrySet()) {
            String userId = entry.getKey();
            String slotId = entry.getValue();
            PooledSession pooled = sessionPool.get(slotId);
            if (pooled == null || !pooled.isActive()) {
                failCount++;
                continue;
            }
            Session session = pooled.getSession();
            if (session == null || !session.isOpen()) {
                log.warn("群发消息 - 用户Session已关闭: userId={}", userId);
                failCount++;
                continue;
            }
            try {
                session.getBasicRemote().sendText(message);
                pooled.updateLastActiveTime();
                successCount++;
            } catch (IOException e) {
                log.error("群发消息 - 发送失败: userId={}, error={}", userId, e.getMessage(), e);
                failCount++;
            }
        }
        log.info("群发消息完成: 成功={}, 失败={}, 总在线用户数={}", successCount, failCount, getActiveCount());
    }

    /**
     * 群发消息给指定用户列表
     *
     * @param userIds 目标用户ID列表
     * @param message 消息内容
     */
    public void broadcastToUsers(Collection<String> userIds, String message) {
        int successCount = 0;
        int failCount = 0;
        for (String userId : userIds) {
            String slotId = userSlotMap.get(userId);
            if (slotId == null) {
                log.warn("群发消息 - 用户不在线: userId={}", userId);
                failCount++;
                continue;
            }
            PooledSession pooled = sessionPool.get(slotId);
            if (pooled == null || !pooled.isActive()) {
                log.warn("群发消息 - 用户不在活跃状态: userId={}", userId);
                failCount++;
                continue;
            }
            Session session = pooled.getSession();
            if (session == null || !session.isOpen()) {
                log.warn("群发消息 - 用户Session已关闭: userId={}", userId);
                failCount++;
                continue;
            }
            try {
                session.getBasicRemote().sendText(message);
                pooled.updateLastActiveTime();
                successCount++;
            } catch (IOException e) {
                log.error("群发消息 - 发送失败: userId={}, error={}", userId, e.getMessage(), e);
                failCount++;
            }
        }
        log.info("指定用户群发消息完成: 成功={}, 失败={}, 目标用户数={}", successCount, failCount, userIds.size());
    }

    /**
     * 点对点发送消息给指定用户
     *
     * @param userId  目标用户ID
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean sendToUser(String userId, String message) {
        String slotId = userSlotMap.get(userId);
        if (slotId == null) {
            log.warn("点对点发送 - 用户不在线: userId={}", userId);
            return false;
        }
        PooledSession pooled = sessionPool.get(slotId);
        if (pooled == null || !pooled.isActive()) {
            log.warn("点对点发送 - 用户不在活跃状态: userId={}", userId);
            return false;
        }
        Session session = pooled.getSession();
        if (session == null || !session.isOpen()) {
            log.warn("点对点发送 - 用户Session已关闭: userId={}", userId);
            return false;
        }
        try {
            session.getBasicRemote().sendText(message);
            pooled.updateLastActiveTime();
            log.debug("点对点发送成功: userId={}", userId);
            return true;
        } catch (IOException e) {
            log.error("点对点发送 - 发送失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }

    // ========== 内部方法 ==========

    /**
     * 定期健康检查：清理已断开但未正确释放的槽位
     */
    private void healthCheck() {
        int cleaned = 0;
        for (Map.Entry<String, String> entry : userSlotMap.entrySet()) {
            String userId = entry.getKey();
            String slotId = entry.getValue();
            PooledSession pooled = sessionPool.get(slotId);
            if (pooled == null) {
                userSlotMap.remove(userId);
                cleaned++;
                continue;
            }
            Session session = pooled.getSession();
            if (session == null || !session.isOpen()) {
                pooled.release();
                idleQueue.offer(slotId);
                userSlotMap.remove(userId);
                cleaned++;
                log.info("健康检查 - 回收断开的槽位: userId={}, slotId={}", userId, slotId);
            }
        }
        if (cleaned > 0) {
            log.info("健康检查完成，回收 {} 个断开槽位，当前活跃用户数: {}, 空闲槽位数: {}",
                    cleaned, getActiveCount(), getIdleCount());
        }
    }
}