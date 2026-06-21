package com.whu.shoppingplatform.dto;

import java.io.Serializable;

public class SeckillMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private Long userId;
    private Long goodsId;
    private Integer quantity;
    private Long timestamp;

    public SeckillMessage() {
    }

    public SeckillMessage(String orderNo, Long userId, Long goodsId, Integer quantity) {
        this.orderNo = orderNo;
        this.userId = userId;
        this.goodsId = goodsId;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}