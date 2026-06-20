package com.whu.shoppingplatform.mapper;

import com.whu.shoppingplatform.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoodsMapper {

    List<Goods> findAll(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    int countAll(@Param("keyword") String keyword);

    Goods findById(@Param("id") Long id);

    int insert(Goods goods);

    int update(Goods goods);

    int deleteById(@Param("id") Long id);
}