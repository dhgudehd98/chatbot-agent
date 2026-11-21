package com.sh.commercechatbot.config;

import com.sh.commercechatbot.agent.ChatAgent;
import com.sh.commercechatbot.enums.chatbot.request.ChatType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class ChatAgentConfig {

    @Bean
    public Map<ChatType, ChatAgent> chatAgents(List<ChatAgent> chatAgentList) {
        return chatAgentList
                .stream()
                .collect(Collectors.toMap(chatAgent -> chatAgent.getChatType(), Function.identity()));
    }
}