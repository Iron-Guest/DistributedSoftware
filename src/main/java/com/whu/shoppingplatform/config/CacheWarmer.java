package com.whu.shoppingplatform.config;

import com.whu.shoppingplatform.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CacheWarmer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmer.class);

    @Autowired
    private CacheService cacheService;

    @Override
    public void run(String... args) {
        log.info("开始缓存预热...");
        try {
            cacheService.warmUpGoodsCache();
            log.info("缓存预热完成");
        } catch (Exception e) {
            log.warn("缓存预热失败: {}", e.getMessage());
        }
    }
}