package com.whu.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping("/fallback")
    public Mono<Map<String, Object>> fallback() {
        Map<String, Object> body = new HashMap<>();
        body.put("code", 503);
        body.put("message", "服务暂时不可用，请稍后重试");
        body.put("data", null);
        return Mono.just(body);
    }
}