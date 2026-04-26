package top.xeonwang.JudgeServer.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.xeonwang.JudgeServer.entity.auth.RedisPrefixConstants;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 指定缓存过期时间
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    // 获取过期时间
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    // 判断 key 是否存在
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    // 删除缓存
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(Arrays.asList(key));
            }
        }
    }

    // 获取缓存
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    // 放入缓存
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    // 放入缓存并设置时间
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    // 放入缓存并设置时间
    public boolean set(String key, Object value, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    /**
     * 尝试获取用户任务锁（原子操作，并发安全）
     *
     * @param userId      用户ID
     * @param operationId 业务类型
     * @return true=获取成功（可执行任务），false=已有任务在执行（拒绝）
     */
    public boolean tryLock(String userId, String operationId) {
        String lockKey = RedisPrefixConstants.LOCK_KEY_PREFIX + operationId + userId;
        // SETNX 原子命令：仅当key不存在时，才设置值+过期时间，防止死锁
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(
                        lockKey,       // 锁Key
                        "executing"    // 锁Value（无实际意义，仅标记状态）
                )
        );
    }

    /**
     * 释放用户任务锁（任务完成/异常必须调用）
     *
     * @param userId 用户ID
     */
    public void unlock(String userId, String operationId) {
        String lockKey = RedisPrefixConstants.LOCK_KEY_PREFIX + operationId + userId;
        redisTemplate.delete(lockKey);
    }
}