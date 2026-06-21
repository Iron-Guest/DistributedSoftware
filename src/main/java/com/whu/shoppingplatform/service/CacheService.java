package com.whu.shoppingplatform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.shoppingplatform.entity.Goods;
import com.whu.shoppingplatform.entity.Stock;
import com.whu.shoppingplatform.mapper.GoodsMapper;
import com.whu.shoppingplatform.mapper.StockMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final Random RANDOM = new Random();

    private static final String GOODS_KEY_PREFIX = "goods:detail:";
    private static final String GOODS_LIST_KEY_PREFIX = "goods:list:";
    private static final String LOCK_KEY_PREFIX = "lock:goods:";
    private static final String NULL_VALUE = "__NULL__";

    private static final long GOODS_TTL_MINUTES = 30;
    private static final long GOODS_TTL_MAX_RANDOM = 10;
    private static final long NULL_TTL_MINUTES = 5;
    private static final long LOCK_TTL_SECONDS = 10;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final GoodsMapper goodsMapper;
    private final StockMapper stockMapper;

    public CacheService(RedisTemplate<String, Object> redisTemplate,
                        ObjectMapper objectMapper,
                        GoodsMapper goodsMapper,
                        StockMapper stockMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.goodsMapper = goodsMapper;
        this.stockMapper = stockMapper;
    }

    public void warmUpGoodsCache() {
        List<Goods> goodsList = goodsMapper.findAll(null, 0, 100);
        for (Goods goods : goodsList) {
            Stock stock = stockMapper.findByGoodsId(goods.getId());
            if (stock != null) {
                goods.setAvailableStock(stock.getTotalStock() - stock.getLockedStock() - stock.getSoldCount());
            }
            String key = GOODS_KEY_PREFIX + goods.getId();
            long ttl = GOODS_TTL_MINUTES + RANDOM.nextInt((int) GOODS_TTL_MAX_RANDOM + 1);
            redisTemplate.opsForValue().set(key, goods, ttl, TimeUnit.MINUTES);
        }
        log.info("已预热 {} 个商品到缓存", goodsList.size());
    }

    public Goods getGoodsWithCache(Long goodsId) {
        String key = GOODS_KEY_PREFIX + goodsId;
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            if (NULL_VALUE.equals(cached)) {
                return null;
            }
            return objectMapper.convertValue(cached, Goods.class);
        }

        String lockKey = LOCK_KEY_PREFIX + goodsId;
        boolean locked = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS));

        if (locked) {
            try {
                cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    if (NULL_VALUE.equals(cached)) {
                        return null;
                    }
                    return objectMapper.convertValue(cached, Goods.class);
                }

                Goods goods = goodsMapper.findById(goodsId);
                if (goods != null) {
                    Stock stock = stockMapper.findByGoodsId(goodsId);
                    if (stock != null) {
                        goods.setAvailableStock(stock.getTotalStock() - stock.getLockedStock() - stock.getSoldCount());
                    }
                    long ttl = GOODS_TTL_MINUTES + RANDOM.nextInt((int) GOODS_TTL_MAX_RANDOM + 1);
                    redisTemplate.opsForValue().set(key, goods, ttl, TimeUnit.MINUTES);
                    log.debug("缓存命中失败，已从DB加载商品: {}", goodsId);
                } else {
                    redisTemplate.opsForValue().set(key, NULL_VALUE, NULL_TTL_MINUTES, TimeUnit.MINUTES);
                    log.debug("商品不存在，已缓存空值: {}", goodsId);
                }
                return goods;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                Thread.sleep(50 + RANDOM.nextInt(100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getGoodsWithCache(goodsId);
        }
    }

    public void evictGoodsCache(Long goodsId) {
        redisTemplate.delete(GOODS_KEY_PREFIX + goodsId);
        redisTemplate.delete(GOODS_LIST_KEY_PREFIX + "*");
        log.debug("已清除商品缓存: {}", goodsId);
    }

    public void evictAllGoodsCache() {
        redisTemplate.delete(redisTemplate.keys(GOODS_KEY_PREFIX + "*"));
        redisTemplate.delete(redisTemplate.keys(GOODS_LIST_KEY_PREFIX + "*"));
    }
}