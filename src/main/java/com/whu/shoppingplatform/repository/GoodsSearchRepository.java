package com.whu.shoppingplatform.repository;

import com.whu.shoppingplatform.entity.GoodsDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsSearchRepository extends ElasticsearchRepository<GoodsDoc, Long> {

    List<GoodsDoc> findByNameContainingOrDescriptionContaining(String name, String description);

    List<GoodsDoc> findByNameContaining(String name);
}