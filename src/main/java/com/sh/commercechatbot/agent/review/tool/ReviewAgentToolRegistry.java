package com.sh.commercechatbot.agent.review.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sh.commercechatbot.chat.repository.ChatSessionRepository;
import org.springframework.ai.tool.annotation.Tool;
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

            // 파라미터 전부를 LLM이 작성해도 되므로 메서드에서 스키마를 자동 생성
            ToolDefinition definition = ToolDefinition.from(
                    ReviewAgentToolService.class.getMethod("searchProductByName", String.class));

            GptToolDefinition search = new GptToolDefinition("function",
                    new GptToolDefinition.Function(definition.name(), definition.description(),
                            mapper.readTree(definition.inputSchema())));

            /**
             * variantId에 대한 값만 LLM이 직접 삽입
             * memberId, SessionRepository에 대한 값은 서버에서 직접 삽입
             * Tool에 대한 어노테이션 즉, description에 대한 값만 가져오고 스키마에 대한 값은 직접 생성
             */
            Tool confirmation = ReviewAgentToolService.class.getMethod("confirmProductSelection",
                    Long.class, Long.class, ChatSessionRepository.class).getAnnotation(Tool.class);

            // 확정 메서드의 memberId·Repository는 서버 컨텍스트이므로 스키마에 포함하지 않음. required에 설정된 변수값이 LLM에서 직접 대입 할 변수으 ㅣ값
            JsonNode confirmationSchema = mapper.readTree("""
                    {"type":"object","properties":{"variantId":{"type":"integer","minimum":1,
                    "description":"사용자가 선택한 현재 세션 후보의 Variant ID"}},
                    "required":["variantId"],"additionalProperties":false}
                    """);
            GptToolDefinition confirm = new GptToolDefinition("function",
                    new GptToolDefinition.Function("confirmProductSelection", confirmation.description(), confirmationSchema));
            this.definitions = List.of(search, confirm);
        } catch (Exception exception) {
            throw new IllegalStateException("Tool definition configuration failed");
        }
    }

    public List<GptToolDefinition> definitions() {
        return definitions;
    }

    /** 허용된 Tool의 입력을 검증하고 Reactive 실행 결과를 Agent에 반환한다. */
    public Mono<ProductSearchResultRecord> execute(
            GptToolCall call, Long memberId, ChatSessionRepository sessionRepository)
    {
        return Mono.defer(() -> {
            if (call == null || call.id() == null || call.id().isBlank()
                    || !"function".equals(call.type()) || call.function() == null
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
            if (arguments == null || !arguments.isObject() || arguments.size() != 1) {
                throw invalidTool();
            }
            return switch (call.function().name()) {
                case "searchProductByName" -> {
                    if (!arguments.has("productName") || !arguments.get("productName").isTextual()) {
                        throw invalidTool();
                    }
                    String productName = arguments.get("productName").asText().strip();
                    if (productName.isEmpty() || productName.length() > 200) {
                        throw invalidTool();
                    }
                    yield service.searchProductByName(productName);
                }
                case "confirmProductSelection" -> {
                    JsonNode id = arguments.get("variantId");
                    if (id == null || !id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() <= 0) {
                        throw invalidTool();
                    }
                    yield service.confirmProductSelection(memberId, id.asLong(), sessionRepository);
                }
                default -> throw invalidTool();
            };
        });
    }

    private ChatbotException invalidTool() {
        return new ChatbotException("Tool 요청을 처리할 수 없습니다.", ChatbotErrorType.INVALID_CHAT_REQUEST);
    }
}
