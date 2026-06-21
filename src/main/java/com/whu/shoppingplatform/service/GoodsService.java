package com.whu.shoppingplatform.service;

import com.whu.shoppingplatform.config.ReadOnly;
import com.whu.shoppingplatform.entity.Goods;
import com.whu.shoppingplatform.entity.Stock;
import com.whu.shoppingplatform.mapper.GoodsMapper;
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
    private final CacheService cacheService;
    private final GoodsSearchService searchService;

    public GoodsService(GoodsMapper goodsMapper,
                        StockMapper stockMapper,
                        CacheService cacheService,
                        @Autowired(required = false) GoodsSearchService searchService) {
        this.goodsMapper = goodsMapper;
        this.stockMapper = stockMapper;
        this.cacheService = cacheService;
        this.searchService = searchService;
    }

    @ReadOnly
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

    @ReadOnly
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

        try {
            if (searchService != null) {
                searchService.indexGoods(goods);
            }
        } catch (Exception e) {
            // ES 索引失败不影响主流程
        }

        return goods;
    }

    @Transactional
    public void deleteGoods(Long id) {
        Goods goods = goodsMapper.findById(id);
        if (goods == null) {
            throw new RuntimeException("商品不存在");
        }
        stockMapper.deleteByGoodsId(id);
        goodsMapper.deleteById(id);

        cacheService.evictGoodsCache(id);

        try {
            if (searchService != null) {
                searchService.deleteGoodsIndex(id);
            }
        } catch (Exception e) {
            // ES 删除失败不影响主流程
        }
    }
}