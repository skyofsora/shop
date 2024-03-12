package com.shop.dto;

import com.shop.constant.ItemSellStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemSearchDto {
    private String searchDateType;  // 조회 날짜
    private ItemSellStatus searchSellStatus;
    private String searchBy;
    private String searchQuery = "";
}
