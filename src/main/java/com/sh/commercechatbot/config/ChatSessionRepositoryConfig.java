package com.sh.commercechatbot.config;

import com.sh.commercechatbot.chat.repository.ChatSessionRepository;
import com.sh.commercechatbot.enums.chatbot.request.ChatType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class ChatSessionRepositoryConfig {
    @Bean
    public Map<ChatType, ChatSessionRepository> chatSessionRepositories(
            List<ChatSessionRepository> repositories) {
        return repositories.stream().collect(Collectors.toUnmodifiableMap(
                ChatSessionRepository::getChatType, Function.identity()));
    }
}
