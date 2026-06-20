package com.whu.shoppingplatform.controller;

import com.whu.shoppingplatform.dto.ApiResponse;
import com.whu.shoppingplatform.dto.CreateOrderRequest;
import com.whu.shoppingplatform.entity.Order;
import com.whu.shoppingplatform.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
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

    @GetMapping("/{id}")
    public ApiResponse<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
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