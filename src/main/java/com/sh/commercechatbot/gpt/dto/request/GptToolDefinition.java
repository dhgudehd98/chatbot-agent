package com.sh.commercechatbot.gpt.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

public record GptToolDefinition(String type, Function function) {
    public record Function(String name, String description, JsonNode parameters) { }
}
