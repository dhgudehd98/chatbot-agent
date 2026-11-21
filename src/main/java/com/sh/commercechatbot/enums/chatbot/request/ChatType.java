package com.sh.commercechatbot.enums.chatbot.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatType {
    REVIEW("리뷰 챗봇"),
    PRODUCT("상품 챗봇"),
    CUSTOMER("고객 챗봇");

    private String agentKind;
}
