package com.whu.shoppingplatform.config;

import com.whu.shoppingplatform.entity.Stock;
import com.whu.shoppingplatform.mapper.StockMapper;
import com.whu.shoppingplatform.service.RedisStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StockInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StockInitializer.class);

    private final StockMapper stockMapper;
    private final RedisStockService redisStockService;

    public StockInitializer(StockMapper stockMapper, RedisStockService redisStockService) {
        this.stockMapper = stockMapper;
        this.redisStockService = redisStockService;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing Redis stock cache...");
        List<Stock> stocks = stockMapper.findAll();
        for (Stock stock : stocks) {
            redisStockService.initStock(stock.getGoodsId(), stock.getTotalStock());
        }
        log.info("Redis stock cache initialized, {} items loaded", stocks.size());
    }
}