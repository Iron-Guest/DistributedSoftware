package com.whu.shoppingplatform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class RedisStockService {

    private static final Logger log = LoggerFactory.getLogger(RedisStockService.class);
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String DEDUP_KEY_PREFIX = "seckill:dedup:";
    private static final long DEDUP_EXPIRE_SECONDS = 3600;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> stockDeductScript;

    public RedisStockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stockDeductScript = new DefaultRedisScript<>();
        this.stockDeductScript.setLocation(new ClassPathResource("redis-stock-deduct.lua"));
        this.stockDeductScript.setResultType(Long.class);
    }

    public void initStock(Long goodsId, int totalStock) {
        String key = STOCK_KEY_PREFIX + goodsId;
        redisTemplate.opsForHash().put(key, "totalStock", String.valueOf(totalStock));
        redisTemplate.opsForHash().put(key, "lockedStock", "0");
        redisTemplate.opsForHash().put(key, "soldCount", "0");
        log.info("Redis stock initialized for goodsId={}, totalStock={}", goodsId, totalStock);
    }

    public void removeStock(Long goodsId) {
        String key = STOCK_KEY_PREFIX + goodsId;
        redisTemplate.delete(key);
        log.info("Redis stock removed for goodsId={}", goodsId);
    }

    public boolean checkIdempotent(Long userId, Long goodsId) {
        String key = DEDUP_KEY_PREFIX + userId + ":" + goodsId;
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", DEDUP_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    public void releaseIdempotent(Long userId, Long goodsId) {
        String key = DEDUP_KEY_PREFIX + userId + ":" + goodsId;
        redisTemplate.delete(key);
    }

    public boolean deductStock(Long goodsId, int quantity) {
        String key = STOCK_KEY_PREFIX + goodsId;
        try {
            Long result = redisTemplate.execute(
                    stockDeductScript,
                    Collections.singletonList(key),
                    String.valueOf(quantity)
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("Redis deductStock failed for goodsId={}", goodsId, e);
            return false;
        }
    }

    public void rollbackStock(Long goodsId, int quantity) {
        String key = STOCK_KEY_PREFIX + goodsId;
        try {
            redisTemplate.opsForHash().increment(key, "lockedStock", -quantity);
        } catch (Exception e) {
            log.error("Redis rollbackStock failed for goodsId={}", goodsId, e);
        }
    }

    public void confirmStock(Long goodsId, int quantity) {
        String key = STOCK_KEY_PREFIX + goodsId;
        try {
            redisTemplate.opsForHash().increment(key, "lockedStock", -quantity);
            redisTemplate.opsForHash().increment(key, "soldCount", quantity);
        } catch (Exception e) {
            log.error("Redis confirmStock failed for goodsId={}", goodsId, e);
        }
    }

    public void restoreStock(Long goodsId, int quantity) {
        String key = STOCK_KEY_PREFIX + goodsId;
        try {
            redisTemplate.opsForHash().increment(key, "soldCount", -quantity);
        } catch (Exception e) {
            log.error("Redis restoreStock failed for goodsId={}", goodsId, e);
        }
    }

    public int getAvailableStock(Long goodsId) {
        String key = STOCK_KEY_PREFIX + goodsId;
        try {
            String total = (String) redisTemplate.opsForHash().get(key, "totalStock");
            String locked = (String) redisTemplate.opsForHash().get(key, "lockedStock");
            String sold = (String) redisTemplate.opsForHash().get(key, "soldCount");
            int t = total != null ? Integer.parseInt(total) : 0;
            int l = locked != null ? Integer.parseInt(locked) : 0;
            int s = sold != null ? Integer.parseInt(sold) : 0;
            return t - l - s;
        } catch (Exception e) {
            log.error("Redis getAvailableStock failed for goodsId={}", goodsId, e);
            return 0;
        }
    }
}