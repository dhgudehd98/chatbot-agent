package com.sh.commercechatbot.common.exception.chatbot;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatbotErrorType {

    GPT_RESPONSE_ERROR(1),
    INVALID_CHAT_REQUEST(2),
    CHAT_SESSION_READ_FAILED(3),
    PRODUCT_SELECTION_NOT_SUPPORTED(4),
    LLM_TIMEOUT(5),
    LLM_BUSY(6),
    LLM_RATE_LIMITED(7);

    private int code;
}