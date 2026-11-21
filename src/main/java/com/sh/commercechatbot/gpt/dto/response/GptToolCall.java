package com.sh.commercechatbot.gpt.dto.response;

public record GptToolCall(String id, String type, Function function) {
    public record Function(String name, String arguments) { }
}
