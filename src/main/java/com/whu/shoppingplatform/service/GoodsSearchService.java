package com.whu.shoppingplatform.service;

import com.whu.shoppingplatform.entity.Goods;
import com.whu.shoppingplatform.entity.GoodsDoc;
import com.whu.shoppingplatform.mapper.GoodsMapper;
import com.whu.shoppingplatform.mapper.StockMapper;
import com.whu.shoppingplatform.repository.GoodsSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnBean(ElasticsearchOperations.class)
public class GoodsSearchService {

    private static final Logger log = LoggerFactory.getLogger(GoodsSearchService.class);

    private final GoodsSearchRepository searchRepository;
    private final ElasticsearchOperations esOperations;
    private final GoodsMapper goodsMapper;
    private final StockMapper stockMapper;

    public GoodsSearchService(GoodsSearchRepository searchRepository,
                               ElasticsearchOperations esOperations,
                               GoodsMapper goodsMapper,
                               StockMapper stockMapper) {
        this.searchRepository = searchRepository;
        this.esOperations = esOperations;
        this.goodsMapper = goodsMapper;
        this.stockMapper = stockMapper;
    }

    public void indexGoods(Goods goods) {
        GoodsDoc doc = toDoc(goods);
        searchRepository.save(doc);
        log.debug("商品已索引到ES: {}", goods.getId());
    }

    public void deleteGoodsIndex(Long goodsId) {
        searchRepository.deleteById(goodsId);
        log.debug("已从ES删除商品索引: {}", goodsId);
    }

    public List<Goods> search(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        Criteria criteria = new Criteria("name").contains(keyword)
                .or(new Criteria("description").contains(keyword));
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(page - 1, size));

        SearchHits<GoodsDoc> hits = esOperations.search(query, GoodsDoc.class);
        return hits.getSearchHits().stream()
                .map(hit -> toGoods(hit.getContent()))
                .collect(Collectors.toList());
    }

    public long countSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return 0;
        }
        Criteria criteria = new Criteria("name").contains(keyword)
                .or(new Criteria("description").contains(keyword));
        CriteriaQuery query = new CriteriaQuery(criteria);
        return esOperations.count(query, GoodsDoc.class);
    }

    public void reindexAll() {
        log.info("开始全量重建ES索引...");
        searchRepository.deleteAll();
        List<Goods> goodsList = goodsMapper.findAll(null, 0, 1000);
        for (Goods goods : goodsList) {
            indexGoods(goods);
        }
        log.info("ES索引重建完成，共 {} 条", goodsList.size());
    }

    private GoodsDoc toDoc(Goods goods) {
        GoodsDoc doc = new GoodsDoc();
        doc.setId(goods.getId());
        doc.setName(goods.getName());
        doc.setDescription(goods.getDescription());
        doc.setPrice(goods.getPrice());
        doc.setImageUrl(goods.getImageUrl());
        doc.setStatus(goods.getStatus());
        doc.setAvailableStock(goods.getAvailableStock());
        return doc;
    }

    private Goods toGoods(GoodsDoc doc) {
        Goods goods = new Goods();
        goods.setId(doc.getId());
        goods.setName(doc.getName());
        goods.setDescription(doc.getDescription());
        goods.setPrice(doc.getPrice());
        goods.setImageUrl(doc.getImageUrl());
        goods.setStatus(doc.getStatus());
        goods.setAvailableStock(doc.getAvailableStock());
        return goods;
    }
}