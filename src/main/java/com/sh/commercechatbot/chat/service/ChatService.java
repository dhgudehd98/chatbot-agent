package com.sh.commercechatbot.chat.service;

import com.sh.commercechatbot.agent.ChatAgent;
import com.sh.commercechatbot.chat.dto.request.*;
import com.sh.commercechatbot.chat.dto.response.ChatResponseDto;
import com.sh.commercechatbot.common.exception.chatbot.*;
import com.sh.commercechatbot.enums.chatbot.request.ChatType;
import com.sh.commercechatbot.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sh.commercechatbot.llm.dto.request.LlmChatRequestDto;
import com.sh.commercechatbot.llm.enums.LlmModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final Map<ChatType, ChatSessionRepository> chatSessionRepositories;
    private final Map<ChatType, ChatAgent> chatAgents;
    private final ObjectMapper objectMapper;

    @Value("${chat.input.max-message-chars:2000}")
    private int maxMessageChars = 2000;
    @Value("${chat.input.max-history-messages:40}")
    private int maxHistoryMessages = 40;
    @Value("${chat.input.max-history-chars:20000}")
    private int maxHistoryChars = 20000;
    @Value("${chat.session.read-timeout:2s}")
    private Duration sessionReadTimeout = Duration.ofSeconds(2);

    public Mono<ChatResponseDto> chat(Long memberId, ChatRequestDto request) {

        return Mono.defer(() -> {
            if (request == null || request.getChatType() == null) {
                return Mono.error(invalidRequest());
            }

            ChatAgent selectedAgent = chatAgents.get(request.getChatType());
            ChatSessionRepository selectedRepository = chatSessionRepositories.get(request.getChatType());

            if (selectedAgent == null || selectedRepository == null) {
                return Mono.error(invalidRequest());
            }

            return prepareLlmRequest(memberId, request, selectedRepository)
                    .flatMap(selectedAgent::respond)
                    .map(answer -> new ChatResponseDto(answer, null));
        });
    }


    /**
     * 요청 값 검증 및 저장된 대화내역 조회
     */
    private Mono<LlmChatRequestDto> prepareLlmRequest(Long memberId, ChatRequestDto request,
                                                    ChatSessionRepository repository) {
        return Mono.defer(() -> {
            validateRequest(memberId, request);
            String message = request.getCurrentMessage().strip();
            LlmModel model = request.getLlmModel();
            return repository.findChatSession(memberId)
                    .timeout(sessionReadTimeout)
                    .onErrorMap(error -> sessionError())
                    .switchIfEmpty(Mono.fromSupplier(() -> ChatSessionDto.create(memberId)))
                    .map(session -> prepareSession(memberId, session, message))
                    .flatMap(session -> createLlmRequest(session, model));
        });
    }

    /** 선택 상품과 대화 이력을 LLM 요청으로 변환한다. memberId는 전달하지 않는다. */
    private Mono<LlmChatRequestDto> createLlmRequest(ChatSessionDto session, LlmModel model) {
        return Mono.fromCallable(() -> {
            List<ChatMessageDto> messages = new ArrayList<>();

            // 현재 사용자 메시지는 prepareSession에서 한 번만 추가한다.
            messages.addAll(session.getMessages());
            return new LlmChatRequestDto(null, null, false, model, List.copyOf(messages));
        });
    }

    private void validateRequest(Long memberId, ChatRequestDto request) {
        if (memberId == null || memberId <= 0 || request == null || request.getChatType() == null) {
            throw invalidRequest();
        }
        // 후보 검증 경로 연결 전에는 임의의 선택 ID를 확정 상품으로 받아들이지 않는다.
        if (request.getSelectedVariantId() != null) {
            throw new ChatbotException("상품 선택은 아직 지원하지 않습니다.",
                    ChatbotErrorType.PRODUCT_SELECTION_NOT_SUPPORTED);
        }
        if (request.getLlmModel() == null || request.getCurrentMessage() == null
                || request.getCurrentMessage().isBlank()
                || request.getCurrentMessage().length() > maxMessageChars) {
            throw invalidRequest();
        }
    }

    private ChatSessionDto prepareSession(Long memberId, ChatSessionDto session, String message) {
        if (!Objects.equals(memberId, session.getMemberId())) {
            throw sessionError();
        }

        List<ChatMessageDto> messages = new ArrayList<>();
        long totalChars = message.length();

        if (session.getMessages() != null) {
            if (session.getMessages().size() >= maxHistoryMessages) {
                throw invalidRequest();
            }
            for (ChatMessageDto previous : session.getMessages()) {
                if (previous == null || previous.getContent() == null
                        || previous.getContent().isBlank()
                        || !("USER".equals(previous.getRole())
                        || "ASSISTANT".equals(previous.getRole()))) {
                    throw sessionError();
                }
                totalChars += previous.getContent().length();
                messages.add(new ChatMessageDto(previous.getRole(), previous.getContent()));
            }
        }
        if (totalChars > maxHistoryChars || messages.size() >= maxHistoryMessages) {
            throw invalidRequest();
        }
        messages.add(new ChatMessageDto("USER", message));
        return new ChatSessionDto(memberId, session.getProductDto(), messages);
    }

    private ChatbotException invalidRequest() {
        return new ChatbotException("상담 요청 또는 대화 길이를 확인해 주세요.",
                ChatbotErrorType.INVALID_CHAT_REQUEST);
    }

    private ChatbotException sessionError() {
        return new ChatbotException("상담 상태를 확인할 수 없습니다.",
                ChatbotErrorType.CHAT_SESSION_READ_FAILED);
    }
}
