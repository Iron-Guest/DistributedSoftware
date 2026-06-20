package com.whu.shoppingplatform.controller;

import com.whu.shoppingplatform.dto.ApiResponse;
import com.whu.shoppingplatform.entity.Stock;
import com.whu.shoppingplatform.service.StockService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{goodsId}")
    public ApiResponse<Stock> getStock(@PathVariable Long goodsId) {
        Stock stock = stockService.getStockByGoodsId(goodsId);
        if (stock == null) {
            return ApiResponse.error(404, "库存记录不存在");
        }
        return ApiResponse.success(stock);
    }
}