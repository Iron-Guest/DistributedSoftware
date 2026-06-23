package com.whu.shoppingplatform.mapper;

import com.whu.shoppingplatform.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    Order findById(@Param("id") Long id);

    Order findByOrderNo(@Param("orderNo") String orderNo);

    List<Order> findByUserId(@Param("userId") Long userId);

    int insert(Order order);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("version") Integer version);

    int deleteByGoodsId(@Param("goodsId") Long goodsId);
}