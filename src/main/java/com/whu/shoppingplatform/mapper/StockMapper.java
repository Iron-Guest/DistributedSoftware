package com.whu.shoppingplatform.mapper;

import com.whu.shoppingplatform.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockMapper {

    Stock findByGoodsId(@Param("goodsId") Long goodsId);

    List<Stock> findAll();

    int insert(Stock stock);

    int deductStock(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity, @Param("version") Integer version);

    int rollbackStock(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity, @Param("version") Integer version);

    int confirmSeckillStock(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity);

    int deleteByGoodsId(@Param("goodsId") Long goodsId);
}