package com.sh.commercechatbot.agent.review.tool;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sh.commercechatbot.chat.repository.ChatSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import com.sh.commercechatbot.agent.review.tool.dto.ReviewToolResult;
import java.time.Duration;
import java.util.Iterator;
import java.util.Set;
import java.util.Objects;
import com.sh.commercechatbot.gpt.dto.request.GptToolDefinition;
import com.sh.commercechatbot.gpt.dto.response.GptToolCall;
import com.sh.commercechatbot.common.exception.chatbot.*;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.List;

/** 활성 Tool의 스키마 제공과 입력 검증. 임의의 메서드 이름을 실행하지 않는다. */
@Component
@Slf4j
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

            ToolDefinition reviewDefinition = ToolDefinition.from(ReviewAgentToolService.class.getMethod(
                    "searchReviewByBodyInfoAndPreferenceQuery", Long.class, Integer.class, Integer.class, String.class));
            GptToolDefinition review = new GptToolDefinition("function",
                    new GptToolDefinition.Function(reviewDefinition.name(), reviewDefinition.description(),
                            mapper.readTree(reviewDefinition.inputSchema())));
            ToolDefinition allReviewDefinition = ToolDefinition.from(ReviewAgentToolService.class.getMethod(
                    "searchReviewAll", Long.class, String.class));
            GptToolDefinition allReviews = new GptToolDefinition("function",
                    new GptToolDefinition.Function(allReviewDefinition.name(), allReviewDefinition.description(),
                            mapper.readTree(allReviewDefinition.inputSchema())));
            this.definitions = List.of(search, confirm, review, allReviews);
        } catch (Exception exception) {
            throw new IllegalStateException("Tool definition configuration failed");
        }
    }

    public List<GptToolDefinition> definitions() {
        return definitions;
    }

    /** 허용된 Tool의 입력을 검증하고 Reactive 실행 결과를 Agent에 반환한다. */
    public Mono<ReviewToolResult> execute(
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
                arguments = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                        .readTree(call.function().arguments());
            } catch (Exception exception) {
                throw invalidTool();
            }

            if (arguments == null || !arguments.isObject() || arguments.size() == 0) {
                throw invalidTool();
            }

            return switch (call.function().name()) {
                case "searchProductByName" -> {
                    if (arguments.size() != 1 || !arguments.has("productName") || !arguments.get("productName").isTextual()) {
                        throw invalidTool();
                    }
                    String productName = arguments.get("productName").asText().strip();
                    if (productName.isEmpty() || productName.length() > 200) {
                        throw invalidTool();
                    }
                    yield service.searchProductByName(productName).map(ReviewToolResult::product);
                }
                case "confirmProductSelection" -> {
                    JsonNode id = arguments.get("variantId");
                    if (arguments.size() != 1 || id == null || !id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() <= 0) {
                        throw invalidTool();
                    }
                    yield service.confirmProductSelection(memberId, id.asLong(), sessionRepository).map(ReviewToolResult::product);
                }
                case "searchReviewByBodyInfoAndPreferenceQuery", "searchReviewAll" -> {
                    boolean withoutBodyInfo = "searchReviewAll".equals(call.function().name());

                    // 허용하는 필드 목록 값 설정
                    Set<String> allowed = withoutBodyInfo
                            ? Set.of("variantId", "preferenceQuery")
                            : Set.of("variantId", "weight", "height", "preferenceQuery");

                    // arguments.fieldNames() : LLM이 실제로 보낸 필드 목록
                    Iterator<String> fields = arguments.fieldNames();

                    while (fields.hasNext()) {
                        if (!allowed.contains(fields.next())) {
                            throw invalidTool();
                        }
                    }
                    JsonNode id = arguments.get("variantId");
                    if (id == null || !id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() <= 0
                            || memberId == null || memberId <= 0 || sessionRepository == null) {
                        throw invalidTool();
                    }

                    Integer weight = optionalPositiveInteger(arguments, "weight");
                    Integer height = optionalPositiveInteger(arguments, "height");

                    JsonNode preference = arguments.get("preferenceQuery");

                    if (preference != null && !preference.isNull()
                            && (!preference.isTextual() || preference.asText().length() > 500)) {
                        throw invalidTool();
                    }

                    String query = preference == null || preference.isNull() ? null : preference.asText().strip();
                    // LLM의 ID를 신뢰하지 않고 현재 세션의 확정 상품과 일치할 때만 조회한다.
                    yield sessionRepository.findChatSession(memberId).timeout(Duration.ofSeconds(2))
                            .switchIfEmpty(Mono.error(invalidTool()))
                            .flatMap(session -> {
                                if (!Objects.equals(memberId, session.getMemberId()) || session.getProductDto() == null
                                        || !Objects.equals(id.asLong(), session.getProductDto().getProductVariantId())) {

                                    return Mono.error(invalidTool());
                                }
                                return (withoutBodyInfo
                                        ? service.searchReviewAll(id.asLong(), query)
                                        : service.searchReviewByBodyInfoAndPreferenceQuery(id.asLong(), weight, height, query))
                                        .map(ReviewToolResult::reviews);
                            });
                }
                default -> throw invalidTool();
            };
        });
    }

    private Integer optionalPositiveInteger(JsonNode arguments, String name) {
        JsonNode value = arguments.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() <= 0
                || value.asInt() > 1000) {
            throw invalidTool();
        }
        return value.asInt();
    }

    private ChatbotException invalidTool() {
        return new ChatbotException("Tool 요청을 처리할 수 없습니다.", ChatbotErrorType.INVALID_CHAT_REQUEST);
    }
}
