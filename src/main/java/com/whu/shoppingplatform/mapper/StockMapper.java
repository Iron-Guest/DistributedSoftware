package com.whu.shoppingplatform.mapper;

import com.whu.shoppingplatform.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockMapper {

    Stock findByGoodsId(@Param("goodsId") Long goodsId);

    int insert(Stock stock);

    int deductStock(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity, @Param("version") Integer version);

    int rollbackStock(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity, @Param("version") Integer version);

    int deleteByGoodsId(@Param("goodsId") Long goodsId);
}