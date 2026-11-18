package com.sh.commercechatbot.chat.agent;

import com.sh.commercechatbot.common.exception.chatbot.ChatbotErrorType;
import com.sh.commercechatbot.common.exception.chatbot.ChatbotException;
import com.sh.commercechatbot.llm.dto.request.LlmChatRequestDto;
import com.sh.commercechatbot.llm.enums.LlmType;
import com.sh.commercechatbot.llm.service.LlmClientService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Semaphore;

/** Tool 없는 초기 상담 Agent. 대화 해석과 추가 질문만 수행한다. */
@Component
public class ChatAgent {
    private final Map<LlmType, LlmClientService> clients;
    private final Duration timeout;
    private final Semaphore permits;

    public ChatAgent(Map<LlmType, LlmClientService> clients,
                     @Value("${chat.agent.timeout:15s}") Duration timeout,
                     @Value("${chat.agent.max-concurrent:20}") int maxConcurrent) {
        if (timeout.isNegative() || timeout.isZero() || maxConcurrent < 1) {
            throw new IllegalArgumentException("Invalid chat agent configuration");
        }
        this.clients = clients;
        this.timeout = timeout;
        this.permits = new Semaphore(maxConcurrent);
    }

    public Mono<String> respond(LlmChatRequestDto request) {
        return Mono.defer(() -> {
            if (request == null || request.getLlmModel() == null
                    || request.getPrompt() == null || request.getPrompt().isBlank()
                    || request.getMessageList() == null || request.getMessageList().isEmpty()) {
                return Mono.error(error(ChatbotErrorType.INVALID_CHAT_REQUEST));
            }
            LlmClientService client = clients.get(request.getLlmModel().getLlmType());
            if (client == null) {
                return Mono.error(error(ChatbotErrorType.INVALID_CHAT_REQUEST));
            }
            // 기다리는 요청 Queue를 만들지 않고 즉시 거절한다.
            if (!permits.tryAcquire()) {
                return Mono.error(error(ChatbotErrorType.LLM_BUSY));
            }
            return Mono.defer(() -> client.getNonStream(request))
                    .switchIfEmpty(Mono.error(error(ChatbotErrorType.GPT_RESPONSE_ERROR)))
                    .flatMap(response -> {
                        if (response.getError() != null || response.getLlmResponse() == null
                                || response.getLlmResponse().isBlank()) {
                            return Mono.error(error(ChatbotErrorType.GPT_RESPONSE_ERROR));
                        }
                        return Mono.just(response.getLlmResponse());
                    })
                    .timeout(timeout, Mono.error(error(ChatbotErrorType.LLM_TIMEOUT)))
                    .onErrorMap(exception -> exception instanceof ChatbotException
                            ? exception : error(ChatbotErrorType.GPT_RESPONSE_ERROR))
                    // 성공·실패·취소 모두 permit 반환. 자동 재시도는 하지 않는다.
                    .doFinally(signal -> permits.release());
        });
    }

    private ChatbotException error(ChatbotErrorType type) {
        return new ChatbotException("상담 요청을 처리할 수 없습니다.", type);
    }
}
