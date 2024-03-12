package com.shop.config;

import com.shop.entity.Member;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString
public class OAuthAttributes {
    private String nameAttributeKey;
    private String name;
    private String email;
    private String picture;
    private String provider;

    @Builder
    public OAuthAttributes(String nameAttributeKey, String name, String email, String picture, String provider) {
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
    }

    public OAuthAttributes() {

    }

    public static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            return ofNaver(attributes);
        } else if ("google".equals(registrationId)) {
            return ofGoogle(attributes);
        } else if ("kakao".equals(registrationId)) {
            return ofKakao(attributes);
        }
        return new OAuthAttributes();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("properties");
        Map<String, Object> kakao_account = (Map<String, Object>) attributes.get("kakao_account");
        return OAuthAttributes.builder()
                .name((String) response.get("nickname"))
                .email(kakao_account.get("email") + "(kakao)")
                .picture((String) response.get("profile_image"))
                .provider("kakao")
                .nameAttributeKey("email")
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email(response.get("email") + "(naver)")
                .picture((String) response.get("profile_image"))
                .provider("naver")
                .nameAttributeKey("email")
                .build();
    }

    public static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email(attributes.get("email") + "(google)")
                .picture((String) attributes.get("picture"))
                .provider("google")
                .nameAttributeKey("email")
                .build();
    }

    public Map<String, Object> map() {
        Map<String, Object> attributesMap = new HashMap<>();
        attributesMap.put("nameAttributeKey", this.nameAttributeKey);
        attributesMap.put("name", this.name);
        attributesMap.put("email", this.email);
        attributesMap.put("picture", this.picture);
        attributesMap.put("provider", this.provider);
        return attributesMap;
    }

    public Member toEntity() {
        return new Member(name, email, picture);
    }
}
