package com.sh.commercechatbot.agent;

import com.sh.commercechatbot.enums.chatbot.request.ChatType;
import com.sh.commercechatbot.llm.dto.request.LlmChatRequestDto;
import reactor.core.publisher.Mono;

public interface ChatAgent {

    ChatType getChatType();

    Mono<String> respond(LlmChatRequestDto chatRequestDto);
}