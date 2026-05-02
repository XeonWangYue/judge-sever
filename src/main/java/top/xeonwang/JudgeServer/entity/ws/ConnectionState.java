package top.xeonwang.JudgeServer.entity.ws;

/**
 * WebSocket连接池中的连接状态
 */
public enum ConnectionState {

    /**
     * 空闲槽位，等待Worker/用户连接
     */
    IDLE,

    /**
     * 已绑定，连接活跃
     */
    ACTIVE,

    /**
     * 已关闭
     */
    CLOSED
}