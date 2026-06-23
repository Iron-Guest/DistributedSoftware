package com.whu.shoppingplatform.service;

import com.whu.shoppingplatform.dto.SeckillMessage;
import com.whu.shoppingplatform.entity.Goods;
import com.whu.shoppingplatform.entity.Order;
import com.whu.shoppingplatform.entity.Stock;
import com.whu.shoppingplatform.mapper.GoodsMapper;
import com.whu.shoppingplatform.mapper.OrderMapper;
import com.whu.shoppingplatform.mapper.StockMapper;
import com.whu.shoppingplatform.messaging.SeckillProducer;
import com.whu.shoppingplatform.util.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    private final RedisStockService redisStockService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final GoodsMapper goodsMapper;
    private final OrderMapper orderMapper;
    private final StockMapper stockMapper;
    private final SeckillProducer seckillProducer;

    public SeckillService(RedisStockService redisStockService,
                          SnowflakeIdGenerator snowflakeIdGenerator,
                          GoodsMapper goodsMapper,
                          OrderMapper orderMapper,
                          StockMapper stockMapper,
                          @Autowired(required = false) SeckillProducer seckillProducer) {
        this.redisStockService = redisStockService;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.goodsMapper = goodsMapper;
        this.orderMapper = orderMapper;
        this.stockMapper = stockMapper;
        this.seckillProducer = seckillProducer;
    }

    public Map<String, Object> seckill(Long userId, Long goodsId, Integer quantity) {
        ensureRedisStockCache(goodsId);

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

        if (seckillProducer != null) {
            SeckillMessage message = new SeckillMessage(orderNo, userId, goodsId, quantity);
            seckillProducer.sendSeckillMessage(message);
            log.info("Seckill order (async): orderNo={}, userId={}, goodsId={}", orderNo, userId, goodsId);
        } else {
            processOrderSynchronously(orderNo, userId, goodsId, quantity);
            log.info("Seckill order (sync): orderNo={}, userId={}, goodsId={}", orderNo, userId, goodsId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("userId", userId);
        result.put("goodsId", goodsId);
        result.put("status", seckillProducer != null ? "processing" : "created");
        return result;
    }

    private void ensureRedisStockCache(Long goodsId) {
        int available = redisStockService.getAvailableStock(goodsId);
        if (available > 0) {
            return;
        }
        Stock stock = stockMapper.findByGoodsId(goodsId);
        if (stock != null) {
            int remaining = stock.getTotalStock() - stock.getSoldCount();
            if (remaining > 0) {
                redisStockService.initStock(goodsId, remaining);
                log.info("Redis stock cache lazy-loaded for goodsId={}, remaining={}", goodsId, remaining);
            }
        }
    }

    @Transactional
    public void processOrderSynchronously(String orderNo, Long userId, Long goodsId, Integer quantity) {
        try {
            Goods goods = goodsMapper.findById(goodsId);
            if (goods == null) {
                throw new RuntimeException("商品不存在: " + goodsId);
            }

            int rows = stockMapper.confirmSeckillStock(goodsId, quantity);
            if (rows <= 0) {
                throw new RuntimeException("MySQL库存扣减失败: goodsId=" + goodsId);
            }

            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setGoodsId(goodsId);
            order.setGoodsName(goods.getName());
            order.setGoodsPrice(goods.getPrice());
            order.setQuantity(quantity);
            order.setTotalAmount(goods.getPrice().multiply(BigDecimal.valueOf(quantity)));
            order.setStatus(1);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            orderMapper.insert(order);
            redisStockService.confirmStock(goodsId, quantity);
        } catch (Exception e) {
            redisStockService.rollbackStock(goodsId, quantity);
            redisStockService.releaseIdempotent(userId, goodsId);
            throw e;
        }
    }
}