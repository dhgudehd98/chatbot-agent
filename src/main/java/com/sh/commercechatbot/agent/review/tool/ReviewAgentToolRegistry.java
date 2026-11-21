package com.sh.commercechatbot.agent.review.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sh.commercechatbot.es.product.dto.ProductSearchResultRecord;
import com.sh.commercechatbot.gpt.dto.request.GptToolDefinition;
import com.sh.commercechatbot.gpt.dto.response.GptToolCall;
import com.sh.commercechatbot.common.exception.chatbot.*;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.List;

/** 활성 Tool의 스키마 제공과 입력 검증. 임의의 메서드 이름을 실행하지 않는다. */
@Component
public class ReviewAgentToolRegistry {
    private final ReviewAgentToolService service;
    private final ObjectMapper mapper;
    private final List<GptToolDefinition> definitions;

    public ReviewAgentToolRegistry(ReviewAgentToolService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
        try {
            ToolDefinition definition = ToolDefinition.from(
                    ReviewAgentToolService.class.getMethod("searchProductByName", String.class));
            this.definitions = List.of(new GptToolDefinition("function",
                    new GptToolDefinition.Function(definition.name(), definition.description(),
                            mapper.readTree(definition.inputSchema()))));
        } catch (Exception exception) {
            throw new IllegalStateException("Tool definition configuration failed");
        }
    }

    public List<GptToolDefinition> definitions() {
        return definitions;
    }

    /** 허용된 Tool의 입력을 검증하고 Reactive 실행 결과를 Agent에 반환한다. */
    public Mono<ProductSearchResultRecord> execute(
            GptToolCall call)
    {
        return Mono.defer(() -> {
            if (call == null || call.id() == null || call.id().isBlank()
                    || !"function".equals(call.type()) || call.function() == null
                    || !"searchProductByName".equals(call.function().name())
                    || call.function().arguments() == null || call.function().arguments().length() > 2048) {
                throw invalidTool();
            }
            JsonNode arguments;
            try {
                arguments = mapper.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                        .readTree(call.function().arguments());
            } catch (Exception exception) {
                throw invalidTool();
            }
            if (arguments == null || !arguments.isObject() || arguments.size() != 1
                    || !arguments.has("productName") || !arguments.get("productName").isTextual()) {
                throw invalidTool();
            }
            String productName = arguments.get("productName").asText().strip();
            if (productName.isEmpty() || productName.length() > 200) {
                throw invalidTool();
            }
            return service.searchProductByName(productName);
        });
    }

    private ChatbotException invalidTool() {
        return new ChatbotException("Tool 요청을 처리할 수 없습니다.", ChatbotErrorType.INVALID_CHAT_REQUEST);
    }
}
