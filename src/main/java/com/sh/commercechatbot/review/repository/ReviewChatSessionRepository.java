package com.sh.commercechatbot.review.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sh.commercechatbot.chat.dto.request.ChatSessionDto;
import com.sh.commercechatbot.chat.repository.ChatSessionRepository;
import com.sh.commercechatbot.enums.chatbot.request.ChatType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class ReviewChatSessionRepository implements ChatSessionRepository {
    private static final String KEY_PREFIX = "chat:review:session:";
    private static final Duration STATE_TTL = Duration.ofMinutes(60);
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public ChatType getChatType() {
        return ChatType.REVIEW;
    }

    @Override
    public String createKey(Long memberId) {
        return KEY_PREFIX + memberId;
    }

    @Override
    public Mono<ChatSessionDto> findChatSession(Long memberId) {
        return Mono.defer(() -> redisTemplate.opsForValue().get(KEY_PREFIX + memberId))
                .flatMap(value -> Mono.fromCallable(() ->
                        objectMapper.readValue(value, ChatSessionDto.class)));
    }

    @Override
    public Mono<Boolean> save(Long memberId, ChatSessionDto sessionDto) {
        String key = createKey(memberId);

        return Mono.fromCallable(() ->
                        objectMapper.writeValueAsString(sessionDto)
                )
                .flatMap(session ->
                        redisTemplate.opsForValue()
                                .set(key, session, STATE_TTL)
                );
    }
}
