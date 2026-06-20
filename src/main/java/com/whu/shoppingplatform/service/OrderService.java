package com.whu.shoppingplatform.service;

import com.whu.shoppingplatform.entity.Goods;
import com.whu.shoppingplatform.entity.Order;
import com.whu.shoppingplatform.entity.Stock;
import com.whu.shoppingplatform.mapper.GoodsMapper;
import com.whu.shoppingplatform.mapper.OrderMapper;
import com.whu.shoppingplatform.mapper.StockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final GoodsMapper goodsMapper;
    private final StockMapper stockMapper;

    public OrderService(OrderMapper orderMapper, GoodsMapper goodsMapper, StockMapper stockMapper) {
        this.orderMapper = orderMapper;
        this.goodsMapper = goodsMapper;
        this.stockMapper = stockMapper;
    }

    @Transactional
    public Order createOrder(Long userId, Long goodsId, Integer quantity) {
        Goods goods = goodsMapper.findById(goodsId);
        if (goods == null) {
            throw new RuntimeException("商品不存在");
        }

        Stock stock = stockMapper.findByGoodsId(goodsId);
        if (stock == null) {
            throw new RuntimeException("库存记录不存在");
        }

        int availableStock = stock.getTotalStock() - stock.getLockedStock() - stock.getSoldCount();
        if (availableStock < quantity) {
            throw new RuntimeException("库存不足，当前可售库存: " + availableStock);
        }

        int rows = stockMapper.deductStock(goodsId, quantity, stock.getVersion());
        if (rows <= 0) {
            throw new RuntimeException("抢购失败，库存已被其他用户抢完，请重试");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
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
        return order;
    }

    public Order getOrderById(Long id) {
        return orderMapper.findById(id);
    }

    public List<Order> listOrdersByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态不允许取消");
        }

        orderMapper.updateStatus(orderId, 0, null);

        Stock stock = stockMapper.findByGoodsId(order.getGoodsId());
        if (stock != null) {
            stockMapper.rollbackStock(order.getGoodsId(), order.getQuantity(), stock.getVersion());
        }
    }

    private String generateOrderNo() {
        return "SK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}