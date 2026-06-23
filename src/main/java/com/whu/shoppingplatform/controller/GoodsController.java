package com.whu.shoppingplatform.controller;

import com.whu.shoppingplatform.dto.ApiResponse;
import com.whu.shoppingplatform.entity.Goods;
import com.whu.shoppingplatform.service.GoodsSearchService;
import com.whu.shoppingplatform.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;
    private final GoodsSearchService searchService;

    public GoodsController(GoodsService goodsService,
                           @Autowired(required = false) GoodsSearchService searchService) {
        this.goodsService = goodsService;
        this.searchService = searchService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listGoods(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        try {
            Map<String, Object> result = goodsService.listGoods(keyword, page, size);
            return ApiResponse.success(result);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Goods> getGoods(@PathVariable Long id) {
        Goods goods = goodsService.getGoodsById(id);
        if (goods == null) {
            return ApiResponse.error(404, "商品不存在");
        }
        return ApiResponse.success(goods);
    }

    @PostMapping
    public ApiResponse<Goods> createGoods(@RequestBody Map<String, Object> request) {
        try {
            String name = request.get("name") != null ? request.get("name").toString() : null;
            if (name == null || name.trim().isEmpty()) {
                return ApiResponse.error(400, "商品名称不能为空");
            }
            String description = request.get("description") != null ? request.get("description").toString() : "";
            BigDecimal price = BigDecimal.ZERO;
            if (request.get("price") != null) {
                try {
                    price = new BigDecimal(request.get("price").toString());
                } catch (NumberFormatException e) {
                    return ApiResponse.error(400, "价格格式不正确");
                }
            }
            String imageUrl = request.get("imageUrl") != null ? request.get("imageUrl").toString() : "";
            Integer stock = 0;
            if (request.get("stock") != null) {
                try {
                    stock = Integer.parseInt(request.get("stock").toString());
                } catch (NumberFormatException e) {
                    return ApiResponse.error(400, "库存数量格式不正确");
                }
            }

            Goods goods = goodsService.createGoods(name, description, price, imageUrl, stock);
            return ApiResponse.success("添加商品成功", goods);
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGoods(@PathVariable Long id) {
        try {
            goodsService.deleteGoods(id);
            return ApiResponse.success("删除商品成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/reindex")
    public ApiResponse<Void> reindex() {
        if (searchService == null) {
            return ApiResponse.error(503, "ES服务未启用");
        }
        try {
            searchService.reindexAll();
            return ApiResponse.success("ES索引重建成功", null);
        } catch (Exception e) {
            return ApiResponse.error(500, "ES索引重建失败: " + e.getMessage());
        }
    }
}