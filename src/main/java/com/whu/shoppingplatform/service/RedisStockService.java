package com.whu.shoppingplatform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
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

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> stockDeductScript;

    public RedisStockService(RedisTemplate<String, Object> redisTemplate) {
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

    /**
     * 幂等性检查：同一用户同一商品只能秒杀一次
     * @return true=未重复, false=重复请求
     */
    public boolean checkIdempotent(Long userId, Long goodsId) {
        String key = DEDUP_KEY_PREFIX + userId + ":" + goodsId;
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", DEDUP_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放幂等性标记（下单失败时回滚）
     */
    public void releaseIdempotent(Long userId, Long goodsId) {
        String key = DEDUP_KEY_PREFIX + userId + ":" + goodsId;
        redisTemplate.delete(key);
    }

    /**
     * 原子性预扣减库存
     * @return true=扣减成功, false=库存不足
     */
    public boolean deductStock(Long goodsId, int quantity) {
        String key = STOCK_KEY_PREFIX + goodsId;
        Long result = redisTemplate.execute(
                stockDeductScript,
                Collections.singletonList(key),
                String.valueOf(quantity)
        );
        return result != null && result == 1L;
    }

    /**
     * 回滚预扣减库存（Redis 侧）
     */
    public void rollbackStock(Long goodsId, int quantity) {
        String key = STOCK_KEY_PREFIX + goodsId;
        redisTemplate.opsForHash().increment(key, "lockedStock", -quantity);
    }

    /**
     * 确认扣减（将 lockedStock 转为 soldCount）
     */
    public void confirmStock(Long goodsId, int quantity) {
        String key = STOCK_KEY_PREFIX + goodsId;
        redisTemplate.opsForHash().increment(key, "lockedStock", -quantity);
        redisTemplate.opsForHash().increment(key, "soldCount", quantity);
    }

    public int getAvailableStock(Long goodsId) {
        String key = STOCK_KEY_PREFIX + goodsId;
        Object total = redisTemplate.opsForHash().get(key, "totalStock");
        Object locked = redisTemplate.opsForHash().get(key, "lockedStock");
        Object sold = redisTemplate.opsForHash().get(key, "soldCount");
        int t = total != null ? Integer.parseInt(total.toString()) : 0;
        int l = locked != null ? Integer.parseInt(locked.toString()) : 0;
        int s = sold != null ? Integer.parseInt(sold.toString()) : 0;
        return t - l - s;
    }
}