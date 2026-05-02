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
 * Worker WebSocket连接池管理器
 * <p>
 * 在启动时预初始化一批空闲槽位，Worker连接时从池中分配，断开时回收至池中。
 * 避免频繁创建/销毁连接对象，提高连接接入效率。
 */
@Component
@Slf4j
public class WorkerSessionManager {

    /**
     * 连接池总容量（可通过 worker.pool.size 配置）
     */
    @Value("${worker.pool.size:16}")
    private int poolSize;

    /**
     * 连接池: slotId -> PooledSession
     */
    private final Map<String, PooledSession> sessionPool = new ConcurrentHashMap<>();

    /**
     * 业务ID到槽位ID的反向映射: workerId -> slotId
     */
    private final Map<String, String> workerSlotMap = new ConcurrentHashMap<>();

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
            String slotId = "worker-slot-" + i;
            sessionPool.put(slotId, new PooledSession(slotId));
            idleQueue.offer(slotId);
        }
        log.info("Worker连接池初始化完成，总容量: {}", poolSize);

        // 启动定期健康检查（每30秒）
        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "worker-pool-health-check");
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
                    log.warn("Worker连接池 - 关闭Session失败: slotId={}", pooled.getSlotId(), e);
                }
            }
            pooled.markClosed();
        }
        sessionPool.clear();
        workerSlotMap.clear();
        idleQueue.clear();

        if (healthCheckExecutor != null) {
            healthCheckExecutor.shutdown();
        }
        log.info("Worker连接池已销毁");
    }

    /**
     * 从池中分配一个空闲槽位给Worker（IDLE → ACTIVE）
     *
     * @param workerId Worker ID
     * @param session  WebSocket Session
     * @return 分配到的槽位ID，无可用槽位返回null
     */
    public String acquireSlot(String workerId, Session session) {
        // 先检查该Worker是否已经占用槽位
        if (workerSlotMap.containsKey(workerId)) {
            log.warn("Worker连接池 - Worker已占用槽位: workerId={}", workerId);
            return null;
        }

        // 从空闲队列取一个槽位
        String slotId = idleQueue.poll();
        if (slotId == null) {
            log.warn("Worker连接池 - 无可用槽位，拒绝连接: workerId={}, 当前活跃数={}", workerId, getActiveCount());
            return null;
        }

        PooledSession pooled = sessionPool.get(slotId);
        if (pooled == null || !pooled.bind(workerId, session)) {
            // 绑定失败，归还槽位
            idleQueue.offer(slotId);
            log.warn("Worker连接池 - 槽位绑定失败: slotId={}, workerId={}", slotId, workerId);
            return null;
        }

        workerSlotMap.put(workerId, slotId);
        log.info("Worker连接池 - 分配槽位: workerId={}, slotId={}, 当前活跃Worker数: {}, 空闲槽位数: {}",
                workerId, slotId, getActiveCount(), getIdleCount());
        return slotId;
    }

    /**
     * 释放Worker占用的槽位（ACTIVE → IDLE）
     *
     * @param workerId Worker ID
     */
    public void releaseSlot(String workerId) {
        String slotId = workerSlotMap.remove(workerId);
        if (slotId == null) {
            log.debug("Worker连接池 - Worker未占用槽位: workerId={}", workerId);
            return;
        }

        PooledSession pooled = sessionPool.get(slotId);
        if (pooled != null && pooled.release()) {
            idleQueue.offer(slotId);
            log.info("Worker连接池 - 回收槽位: workerId={}, slotId={}, 当前活跃Worker数: {}, 空闲槽位数: {}",
                    workerId, slotId, getActiveCount(), getIdleCount());
        }
    }

    // ========== 兼容旧接口 ==========
    // 保留原有的 addSession/removeSession 作为语义别名，方便其他代码无缝切换

    /**
     * 添加Worker连接（等同于 acquireSlot）
     *
     * @param workerId Worker ID
     * @param session  WebSocket Session
     */
    public void addSession(String workerId, Session session) {
        acquireSlot(workerId, session);
    }

    /**
     * 移除Worker连接（等同于 releaseSlot）
     *
     * @param workerId Worker ID
     */
    public void removeSession(String workerId) {
        releaseSlot(workerId);
    }

    // ========== 查询接口 ==========

    /**
     * 判断Worker是否已有连接
     *
     * @param workerId Worker ID
     * @return 是否存在活跃连接
     */
    public boolean hasSession(String workerId) {
        return workerSlotMap.containsKey(workerId);
    }

    /**
     * 获取Worker的Session
     *
     * @param workerId Worker ID
     * @return Session对象，不存在或空闲则返回null
     */
    public Session getSession(String workerId) {
        String slotId = workerSlotMap.get(workerId);
        if (slotId == null) {
            return null;
        }
        PooledSession pooled = sessionPool.get(slotId);
        return pooled != null ? pooled.getSession() : null;
    }

    /**
     * 获取Worker对应的PooledSession（包含完整池化信息）
     *
     * @param workerId Worker ID
     * @return PooledSession对象
     */
    public PooledSession getPooledSession(String workerId) {
        String slotId = workerSlotMap.get(workerId);
        if (slotId == null) {
            return null;
        }
        return sessionPool.get(slotId);
    }

    /**
     * 获取当前活跃Worker数量
     */
    public int getActiveCount() {
        return workerSlotMap.size();
    }

    /**
     * 获取当前在线Worker数（与 getActiveCount 相同，兼容旧接口）
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
     * 获取所有在线Worker ID列表
     */
    public Collection<String> getAllWorkerIds() {
        return Collections.unmodifiableSet(workerSlotMap.keySet());
    }

    /**
     * 获取连接池中所有槽位的快照（用于监控/调试）
     */
    public Collection<PooledSession> getAllPooledSessions() {
        return Collections.unmodifiableCollection(sessionPool.values());
    }

    // ========== 消息发送接口 ==========

    /**
     * 群发消息给所有在线Worker
     *
     * @param message 消息内容
     */
    public void broadcastToAll(String message) {
        int successCount = 0;
        int failCount = 0;
        for (Map.Entry<String, String> entry : workerSlotMap.entrySet()) {
            String workerId = entry.getKey();
            String slotId = entry.getValue();
            PooledSession pooled = sessionPool.get(slotId);
            if (pooled == null || !pooled.isActive()) {
                failCount++;
                continue;
            }
            Session session = pooled.getSession();
            if (session == null || !session.isOpen()) {
                log.warn("群发消息 - Worker Session已关闭: workerId={}", workerId);
                failCount++;
                continue;
            }
            try {
                session.getBasicRemote().sendText(message);
                pooled.updateLastActiveTime();
                successCount++;
            } catch (IOException e) {
                log.error("群发消息 - 发送失败: workerId={}, error={}", workerId, e.getMessage(), e);
                failCount++;
            }
        }
        log.info("群发消息完成: 成功={}, 失败={}, 总在线Worker数={}", successCount, failCount, getActiveCount());
    }

    /**
     * 点对点发送消息给指定Worker
     *
     * @param workerId 目标Worker ID
     * @param message  消息内容
     * @return 是否发送成功
     */
    public boolean sendToWorker(String workerId, String message) {
        String slotId = workerSlotMap.get(workerId);
        if (slotId == null) {
            log.warn("点对点发送 - Worker不在线: workerId={}", workerId);
            return false;
        }
        PooledSession pooled = sessionPool.get(slotId);
        if (pooled == null || !pooled.isActive()) {
            log.warn("点对点发送 - Worker不在活跃状态: workerId={}", workerId);
            return false;
        }
        Session session = pooled.getSession();
        if (session == null || !session.isOpen()) {
            log.warn("点对点发送 - Worker Session已关闭: workerId={}", workerId);
            return false;
        }
        try {
            session.getBasicRemote().sendText(message);
            pooled.updateLastActiveTime();
            log.debug("点对点发送成功: workerId={}", workerId);
            return true;
        } catch (IOException e) {
            log.error("点对点发送 - 发送失败: workerId={}, error={}", workerId, e.getMessage(), e);
            return false;
        }
    }

    // ========== 内部方法 ==========

    /**
     * 定期健康检查：清理已断开但未正确释放的槽位
     */
    private void healthCheck() {
        int cleaned = 0;
        for (Map.Entry<String, String> entry : workerSlotMap.entrySet()) {
            String workerId = entry.getKey();
            String slotId = entry.getValue();
            PooledSession pooled = sessionPool.get(slotId);
            if (pooled == null) {
                // 清理映射
                workerSlotMap.remove(workerId);
                cleaned++;
                continue;
            }
            Session session = pooled.getSession();
            if (session == null || !session.isOpen()) {
                // Session已关闭，回收槽位
                pooled.release();
                idleQueue.offer(slotId);
                workerSlotMap.remove(workerId);
                cleaned++;
                log.info("健康检查 - 回收断开的槽位: workerId={}, slotId={}", workerId, slotId);
            }
        }
        if (cleaned > 0) {
            log.info("健康检查完成，回收 {} 个断开槽位，当前活跃Worker数: {}, 空闲槽位数: {}",
                    cleaned, getActiveCount(), getIdleCount());
        }
    }
}