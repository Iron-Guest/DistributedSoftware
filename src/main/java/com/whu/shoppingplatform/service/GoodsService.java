package com.whu.shoppingplatform.service;

import com.whu.shoppingplatform.entity.Goods;
import com.whu.shoppingplatform.entity.Stock;
import com.whu.shoppingplatform.mapper.GoodsMapper;
import com.whu.shoppingplatform.mapper.OrderMapper;
import com.whu.shoppingplatform.mapper.StockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoodsService {

    private final GoodsMapper goodsMapper;
    private final StockMapper stockMapper;
    private final OrderMapper orderMapper;
    private final CacheService cacheService;
    private final GoodsSearchService searchService;
    private final RedisStockService redisStockService;

    public GoodsService(GoodsMapper goodsMapper,
                        StockMapper stockMapper,
                        OrderMapper orderMapper,
                        CacheService cacheService,
                        @Autowired(required = false) GoodsSearchService searchService,
                        @Autowired(required = false) RedisStockService redisStockService) {
        this.goodsMapper = goodsMapper;
        this.stockMapper = stockMapper;
        this.orderMapper = orderMapper;
        this.cacheService = cacheService;
        this.searchService = searchService;
        this.redisStockService = redisStockService;
    }

    public Map<String, Object> listGoods(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<Goods> list;
        int total;

        if (keyword != null && !keyword.trim().isEmpty() && searchService != null) {
            try {
                list = searchService.search(keyword, page, size);
                total = (int) searchService.countSearch(keyword);
            } catch (Exception e) {
                list = goodsMapper.findAll(keyword, offset, size);
                total = goodsMapper.countAll(keyword);
            }
        } else {
            list = goodsMapper.findAll(keyword, offset, size);
            total = goodsMapper.countAll(keyword);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public Goods getGoodsById(Long id) {
        Goods goods = cacheService.getGoodsWithCache(id);
        if (goods == null) {
            goods = goodsMapper.findById(id);
        }
        return goods;
    }

    @Transactional
    public Goods createGoods(String name, String description, BigDecimal price,
                              String imageUrl, Integer stockQuantity) {
        Goods goods = new Goods();
        goods.setName(name);
        goods.setDescription(description);
        goods.setPrice(price);
        goods.setImageUrl(imageUrl);
        goods.setStatus(1);
        goods.setCreatedAt(LocalDateTime.now());
        goods.setUpdatedAt(LocalDateTime.now());

        int result = goodsMapper.insert(goods);
        if (result <= 0) {
            throw new RuntimeException("添加商品失败");
        }

        Stock stock = new Stock();
        stock.setGoodsId(goods.getId());
        stock.setTotalStock(stockQuantity != null ? stockQuantity : 0);
        stock.setLockedStock(0);
        stock.setSoldCount(0);
        stock.setVersion(0);
        stock.setUpdatedAt(LocalDateTime.now());

        stockMapper.insert(stock);

        goods.setAvailableStock(stockQuantity);

        if (redisStockService != null && stockQuantity != null && stockQuantity > 0) {
            redisStockService.initStock(goods.getId(), stockQuantity);
        }

        try {
            if (searchService != null) {
                searchService.indexGoods(goods);
            }
        } catch (Exception e) {}

        return goods;
    }

    @Transactional
    public void deleteGoods(Long id) {
        Goods goods = goodsMapper.findById(id);
        if (goods == null) {
            throw new RuntimeException("商品不存在");
        }
        orderMapper.deleteByGoodsId(id);
        stockMapper.deleteByGoodsId(id);
        goodsMapper.deleteById(id);

        cacheService.evictGoodsCache(id);

        if (redisStockService != null) {
            try {
                redisStockService.removeStock(id);
            } catch (Exception e) {}
        }

        try {
            if (searchService != null) {
                searchService.deleteGoodsIndex(id);
            }
        } catch (Exception e) {}
    }
}