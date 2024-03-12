package com.shop.info;

import com.shop.entity.Member;

import java.util.Map;

public interface OAuth2MemberInfo {
    Map<String, Object> getAttributes();
    String getProviderId();

    String Provider();

    String getName();

    String getEmail();
    String getPicture();
}
