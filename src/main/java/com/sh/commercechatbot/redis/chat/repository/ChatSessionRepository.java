package com.sh.commercechatbot.redis.chat.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sh.commercechatbot.chat.dto.request.ChatSessionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatSessionRepository {

    private static final String KEY_PREFIX = "chat:session:memberId:";
    private static final Duration STATE_TTL = Duration.ofMinutes(60);
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper mapper;

    private String create(Long memberId) {


        return KEY_PREFIX + memberId;
    }

    public Mono<ChatSessionDto> findChatSession(Long memberId) {
        // chat:session:memberId:1
        String key = create(memberId);

        return redisTemplate.opsForValue()
                .get(key)
                .map(value ->{
                    try {
                        return mapper.readValue(value, ChatSessionDto.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });


    }
}