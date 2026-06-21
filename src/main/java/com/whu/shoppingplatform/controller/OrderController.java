package com.whu.shoppingplatform.controller;

import com.whu.shoppingplatform.dto.ApiResponse;
import com.whu.shoppingplatform.dto.CreateOrderRequest;
import com.whu.shoppingplatform.entity.Order;
import com.whu.shoppingplatform.service.OrderService;
import com.whu.shoppingplatform.service.SeckillService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final SeckillService seckillService;

    public OrderController(OrderService orderService,
                           @Autowired(required = false) SeckillService seckillService) {
        this.orderService = orderService;
        this.seckillService = seckillService;
    }

    @PostMapping
    public ApiResponse<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        try {
            Order order = orderService.createOrder(request.getUserId(), request.getGoodsId(), request.getQuantity());
            return ApiResponse.success("抢购成功", order);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/seckill")
    public ApiResponse<Map<String, Object>> seckill(@Valid @RequestBody CreateOrderRequest request) {
        if (seckillService == null) {
            return ApiResponse.error(503, "秒杀服务未启用，请检查 Kafka 配置");
        }
        try {
            Map<String, Object> result = seckillService.seckill(
                    request.getUserId(), request.getGoodsId(), request.getQuantity());
            return ApiResponse.success("秒杀下单已提交，处理中", result);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return ApiResponse.error(404, "订单不存在");
        }
        return ApiResponse.success(order);
    }

    @GetMapping("/no/{orderNo}")
    public ApiResponse<Order> getOrderByNo(@PathVariable String orderNo) {
        Order order = orderService.getOrderByOrderNo(orderNo);
        if (order == null) {
            return ApiResponse.error(404, "订单不存在");
        }
        return ApiResponse.success(order);
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Order>> listOrdersByUser(@PathVariable Long userId) {
        List<Order> orders = orderService.listOrdersByUserId(userId);
        return ApiResponse.success(orders);
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable Long id) {
        try {
            orderService.cancelOrder(id);
            return ApiResponse.success("订单已取消", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}