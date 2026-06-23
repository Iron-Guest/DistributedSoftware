package com.whu.shoppingplatform.service;

import com.whu.shoppingplatform.entity.Stock;
import com.whu.shoppingplatform.mapper.StockMapper;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    private final StockMapper stockMapper;

    public StockService(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    public Stock getStockByGoodsId(Long goodsId) {
        return stockMapper.findByGoodsId(goodsId);
    }
}