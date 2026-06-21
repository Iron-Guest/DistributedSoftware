package com.whu.shoppingplatform.service;

import com.whu.shoppingplatform.dto.SeckillMessage;
import com.whu.shoppingplatform.messaging.SeckillProducer;
import com.whu.shoppingplatform.util.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    private final RedisStockService redisStockService;
    private final SeckillProducer seckillProducer;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SeckillService(RedisStockService redisStockService,
                          SeckillProducer seckillProducer,
                          SnowflakeIdGenerator snowflakeIdGenerator) {
        this.redisStockService = redisStockService;
        this.seckillProducer = seckillProducer;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * 秒杀下单主流程
     * 1. 幂等性检查（Redis SET NX）
     * 2. Redis 预扣减库存（Lua 脚本原子操作）
     * 3. 生成订单号（雪花算法）
     * 4. 发送消息到 Kafka
     * 5. 返回订单号
     */
    public Map<String, Object> seckill(Long userId, Long goodsId, Integer quantity) {
        boolean notDup = redisStockService.checkIdempotent(userId, goodsId);
        if (!notDup) {
            throw new RuntimeException("您已参与过该商品的秒杀，请勿重复下单");
        }

        boolean deducted = redisStockService.deductStock(goodsId, quantity);
        if (!deducted) {
            redisStockService.releaseIdempotent(userId, goodsId);
            throw new RuntimeException("库存不足，秒杀失败");
        }

        String orderNo = snowflakeIdGenerator.nextOrderNo();

        SeckillMessage message = new SeckillMessage(orderNo, userId, goodsId, quantity);
        seckillProducer.sendSeckillMessage(message);

        log.info("Seckill order placed: orderNo={}, userId={}, goodsId={}, quantity={}",
                orderNo, userId, goodsId, quantity);

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("userId", userId);
        result.put("goodsId", goodsId);
        result.put("status", "processing");
        return result;
    }
}