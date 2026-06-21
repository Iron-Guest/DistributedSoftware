package com.whu.shoppingplatform.messaging;

import com.whu.shoppingplatform.dto.SeckillMessage;
import com.whu.shoppingplatform.entity.Goods;
import com.whu.shoppingplatform.entity.Order;
import com.whu.shoppingplatform.mapper.GoodsMapper;
import com.whu.shoppingplatform.mapper.OrderMapper;
import com.whu.shoppingplatform.mapper.StockMapper;
import com.whu.shoppingplatform.service.RedisStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class SeckillConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillConsumer.class);

    private final OrderMapper orderMapper;
    private final GoodsMapper goodsMapper;
    private final StockMapper stockMapper;
    private final RedisStockService redisStockService;

    public SeckillConsumer(OrderMapper orderMapper,
                           GoodsMapper goodsMapper,
                           StockMapper stockMapper,
                           RedisStockService redisStockService) {
        this.orderMapper = orderMapper;
        this.goodsMapper = goodsMapper;
        this.stockMapper = stockMapper;
        this.redisStockService = redisStockService;
    }

    @KafkaListener(topics = "seckill-order", groupId = "seckill-group", concurrency = "4")
    @Transactional
    public void onMessage(SeckillMessage message, Acknowledgment ack) {
        log.info("Processing seckill order: orderNo={}, userId={}, goodsId={}",
                message.getOrderNo(), message.getUserId(), message.getGoodsId());

        try {
            processOrder(message);
            ack.acknowledge();
            log.info("Seckill order processed successfully: orderNo={}", message.getOrderNo());
        } catch (Exception e) {
            log.error("Failed to process seckill order: orderNo={}", message.getOrderNo(), e);
            redisStockService.rollbackStock(message.getGoodsId(), message.getQuantity());
            redisStockService.releaseIdempotent(message.getUserId(), message.getGoodsId());
            throw e;
        }
    }

    private void processOrder(SeckillMessage message) {
        Goods goods = goodsMapper.findById(message.getGoodsId());
        if (goods == null) {
            throw new RuntimeException("商品不存在: " + message.getGoodsId());
        }

        int rows = stockMapper.confirmSeckillStock(message.getGoodsId(), message.getQuantity());
        if (rows <= 0) {
            throw new RuntimeException("MySQL库存扣减失败: goodsId=" + message.getGoodsId());
        }

        Order order = new Order();
        order.setOrderNo(message.getOrderNo());
        order.setUserId(message.getUserId());
        order.setGoodsId(message.getGoodsId());
        order.setGoodsName(goods.getName());
        order.setGoodsPrice(goods.getPrice());
        order.setQuantity(message.getQuantity());
        order.setTotalAmount(goods.getPrice().multiply(BigDecimal.valueOf(message.getQuantity())));
        order.setStatus(1);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        orderMapper.insert(order);

        redisStockService.confirmStock(message.getGoodsId(), message.getQuantity());
        log.info("Order created: orderNo={}, goodsName={}", order.getOrderNo(), order.getGoodsName());
    }
}