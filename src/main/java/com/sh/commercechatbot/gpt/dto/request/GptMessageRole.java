package com.sh.commercechatbot.gpt.dto.request;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GptMessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL;

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}