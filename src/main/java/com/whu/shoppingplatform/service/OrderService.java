package com.whu.shoppingplatform.service;

import com.whu.shoppingplatform.config.ReadOnly;
import com.whu.shoppingplatform.entity.Order;
import com.whu.shoppingplatform.entity.Stock;
import com.whu.shoppingplatform.mapper.OrderMapper;
import com.whu.shoppingplatform.mapper.StockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final StockMapper stockMapper;

    public OrderService(OrderMapper orderMapper, StockMapper stockMapper) {
        this.orderMapper = orderMapper;
        this.stockMapper = stockMapper;
    }

    @ReadOnly
    public Order getOrderById(Long id) {
        return orderMapper.findById(id);
    }

    @ReadOnly
    public Order getOrderByOrderNo(String orderNo) {
        return orderMapper.findByOrderNo(orderNo);
    }

    @ReadOnly
    public List<Order> listOrdersByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    @Transactional
    public Order payOrder(Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态不允许支付，当前状态: " + order.getStatus());
        }

        orderMapper.updateStatus(orderId, 2, null);
        order.setStatus(2);
        return order;
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
}