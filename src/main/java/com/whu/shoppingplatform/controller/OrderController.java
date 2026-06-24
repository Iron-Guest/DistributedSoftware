package com.whu.shoppingplatform.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.whu.shoppingplatform.dto.ApiResponse;
import com.whu.shoppingplatform.dto.CreateOrderRequest;
import com.whu.shoppingplatform.entity.Order;
import com.whu.shoppingplatform.service.OrderService;
import com.whu.shoppingplatform.service.SeckillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final SeckillService seckillService;

    public OrderController(OrderService orderService, SeckillService seckillService) {
        this.orderService = orderService;
        this.seckillService = seckillService;
    }

    @PostMapping("/seckill")
    @SentinelResource(value = "seckill-order",
            blockHandler = "seckillBlockHandler",
            fallback = "seckillFallback")
    public ApiResponse<Map<String, Object>> seckill(@Valid @RequestBody CreateOrderRequest request) {
        Map<String, Object> result = seckillService.seckill(
                request.getUserId(), request.getGoodsId(), request.getQuantity());
        return ApiResponse.success("下单成功", result);
    }

    public ApiResponse<Map<String, Object>> seckillBlockHandler(CreateOrderRequest request, BlockException e) {
        return ApiResponse.error(429, "秒杀请求过于频繁，请稍后再试");
    }

    public ApiResponse<Map<String, Object>> seckillFallback(CreateOrderRequest request, Throwable e) {
        return ApiResponse.error(503, "秒杀服务暂时不可用: " + e.getMessage());
    }

    @GetMapping("/{id}")
    @SentinelResource(value = "order-query",
            blockHandler = "orderQueryBlockHandler")
    public ApiResponse<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return ApiResponse.error(404, "订单不存在");
        }
        return ApiResponse.success(order);
    }

    public ApiResponse<Order> orderQueryBlockHandler(Long id, BlockException e) {
        return ApiResponse.error(429, "查询请求过于频繁，请稍后再试");
    }

    @GetMapping("/no/{orderNo}")
    @SentinelResource(value = "order-query",
            blockHandler = "orderQueryBlockHandler2")
    public ApiResponse<Order> getOrderByNo(@PathVariable String orderNo) {
        Order order = orderService.getOrderByOrderNo(orderNo);
        if (order == null) {
            return ApiResponse.error(404, "订单不存在");
        }
        return ApiResponse.success(order);
    }

    public ApiResponse<Order> orderQueryBlockHandler2(String orderNo, BlockException e) {
        return ApiResponse.error(429, "查询请求过于频繁，请稍后再试");
    }

    @GetMapping("/user/{userId}")
    @SentinelResource(value = "order-query",
            blockHandler = "orderListBlockHandler")
    public ApiResponse<List<Order>> listOrdersByUser(@PathVariable Long userId) {
        List<Order> orders = orderService.listOrdersByUserId(userId);
        return ApiResponse.success(orders);
    }

    public ApiResponse<List<Order>> orderListBlockHandler(Long userId, BlockException e) {
        return ApiResponse.error(429, "查询请求过于频繁，请稍后再试");
    }

    @PutMapping("/{id}/pay")
    public ApiResponse<Order> payOrder(@PathVariable Long id) {
        try {
            Order order = orderService.payOrder(id);
            return ApiResponse.success("支付成功", order);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
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