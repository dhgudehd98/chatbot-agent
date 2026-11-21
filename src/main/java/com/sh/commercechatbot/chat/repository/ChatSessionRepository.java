package com.sh.commercechatbot.chat.repository;

import com.sh.commercechatbot.chat.dto.request.ChatSessionDto;
import com.sh.commercechatbot.enums.chatbot.request.ChatType;
import reactor.core.publisher.Mono;

public interface ChatSessionRepository {
    ChatType getChatType();

    Mono<ChatSessionDto> findChatSession(Long memberId);
}
