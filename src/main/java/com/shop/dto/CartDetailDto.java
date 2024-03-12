package com.shop.dto;

import lombok.Data;

@Data
public class CartDetailDto {
    private Long cartItemId;
    private String itemNm;
    private long price;
    private int count;
    private String imgUrl;

    public CartDetailDto(Long cartItemId, String itemNm, long price, int count, String imgUrl) {
        this.cartItemId = cartItemId;
        this.itemNm = itemNm;
        this.price = price;
        this.count = count;
        this.imgUrl = imgUrl;
    }
}
