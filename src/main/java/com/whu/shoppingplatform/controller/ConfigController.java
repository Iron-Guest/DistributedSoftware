package com.whu.shoppingplatform.controller;

import com.whu.shoppingplatform.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RefreshScope
public class ConfigController {

    @Value("${seckill.max-retry:3}")
    private int maxRetry;

    @Value("${seckill.order-timeout-seconds:900}")
    private int orderTimeoutSeconds;

    @GetMapping("/seckill")
    public ApiResponse<Map<String, Object>> getSeckillConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxRetry", maxRetry);
        config.put("orderTimeoutSeconds", orderTimeoutSeconds);
        config.put("timestamp", System.currentTimeMillis());
        return ApiResponse.success(config);
    }
}