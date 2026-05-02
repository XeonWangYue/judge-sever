package top.xeonwang.JudgeServer.entity.ws;

import jakarta.websocket.Session;
import lombok.Getter;

/**
 * 连接池中的Session包装器
 * 记录连接状态、创建时间等元信息
 */
@Getter
public class PooledSession {

    /**
     * 池位槽位ID（用于标识槽位，即使Session被替换也可以追踪）
     */
    private final String slotId;

    /**
     * 当前连接绑定的业务ID（WorkerId / UserId），IDLE时为null
     */
    private volatile String boundId;

    /**
     * 底层WebSocket Session
     */
    private volatile Session session;

    /**
     * 当前连接状态
     */
    private volatile ConnectionState state;

    /**
     * 最后一次心跳/消息时间（毫秒时间戳）
     */
    private volatile long lastActiveTime;

    public PooledSession(String slotId) {
        this.slotId = slotId;
        this.boundId = null;
        this.session = null;
        this.state = ConnectionState.IDLE;
        this.lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 绑定连接（IDLE → ACTIVE）
     *
     * @param boundId 业务ID
     * @param session WebSocket Session
     * @return 是否成功绑定（只有IDLE状态才能绑定）
     */
    public synchronized boolean bind(String boundId, Session session) {
        if (this.state != ConnectionState.IDLE) {
            return false;
        }
        this.boundId = boundId;
        this.session = session;
        this.state = ConnectionState.ACTIVE;
        this.lastActiveTime = System.currentTimeMillis();
        return true;
    }

    /**
     * 释放连接（ACTIVE → IDLE）
     *
     * @return 是否成功释放
     */
    public synchronized boolean release() {
        if (this.state != ConnectionState.ACTIVE) {
            return false;
        }
        this.boundId = null;
        this.session = null;
        this.state = ConnectionState.IDLE;
        this.lastActiveTime = System.currentTimeMillis();
        return true;
    }

    /**
     * 标记为已关闭
     */
    public synchronized void markClosed() {
        this.state = ConnectionState.CLOSED;
        this.boundId = null;
        this.session = null;
    }

    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 是否处于活跃状态
     */
    public boolean isActive() {
        return this.state == ConnectionState.ACTIVE;
    }

    /**
     * 是否空闲
     */
    public boolean isIdle() {
        return this.state == ConnectionState.IDLE;
    }

    @Override
    public String toString() {
        return "PooledSession{" +
                "slotId='" + slotId + '\'' +
                ", boundId='" + boundId + '\'' +
                ", state=" + state +
                ", lastActiveTime=" + lastActiveTime +
                '}';
    }
}