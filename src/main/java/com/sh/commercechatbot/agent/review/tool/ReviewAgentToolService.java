package com.sh.commercechatbot.agent.review.tool;

import com.sh.commercechatbot.chat.dto.request.ChatSessionDto;
import com.sh.commercechatbot.common.exception.chatbot.ChatbotException;
import com.sh.commercechatbot.common.exception.chatbot.ChatbotErrorType;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import com.sh.commercechatbot.chat.dto.request.UserRequestProductDto;
import com.sh.commercechatbot.chat.repository.ChatSessionRepository;
import com.sh.commercechatbot.es.product.dto.ProductSearchResultDto;
import com.sh.commercechatbot.es.product.dto.ProductSearchResultRecord;
import com.sh.commercechatbot.es.product.repositroy.ProductDocumentRepository;
import com.sh.commercechatbot.es.product.service.ProductSearchResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewAgentToolService {
    /**
     * Tool 작성 기준
     * - 실제 상품·옵션·리뷰 등 외부 데이터 확인이 필요한 경우 사용한다.
     * - 활성 Tool은 Registry에 명시적으로 등록한다.
     * - 입력·출력 계약과 조회 로직은 후속 구현에서 확정한다.
     */
    private final ProductDocumentRepository documentRepository;
    private final ProductSearchResultService productSearchResultService;
    @Value("${chat.session.read-timeout:2s}")
    private Duration sessionReadTimeout = Duration.ofSeconds(2);
    @Value("${chat.session.write-timeout:2s}")
    private Duration sessionWriteTimeout = Duration.ofSeconds(2);


    @Tool(description = "사용자가 언급한 상품명으로 상품 후보를 검색한다. 상품 식별 정보가 없으면 호출하지 않고 상품명을 질문한다. 후보가 여러 개면 임의로 확정하지 않고 사용자에게 선택을 요청한다.")
    public Mono<ProductSearchResultRecord> searchProductByName(
            @ToolParam(description = "사용자가 언급한 상품명. 브랜드와 색상을 포함할 수 있다.")String productName
            ) {
        return documentRepository.search(productName)
                .take(5)
                .collectList()
                .map(results -> results.isEmpty()
                        ? ProductSearchResultRecord.notFound("NOT_FOUND")
                        : ProductSearchResultRecord.found(results.size() == 1 ? "SINGLE_FOUND" : "MULTI_FOUND", results));
    }

    @Tool(description = "사용자가 이전 후보를 명시적으로 선택·확인한 경우에만 상품을 확정한다. variantId는 서버 후보 ID만 사용하고 모호하면 질문한다. 후보가 하나라는 이유로 자동 확정하지 않는다.")
    public Mono<ProductSearchResultRecord> confirmProductSelection(
            Long memberId,
            Long variantId,
            ChatSessionRepository sessionRepository)
    {
        return Mono.defer(() -> {
            if (memberId == null || memberId <= 0 || variantId == null || variantId <= 0
                    || sessionRepository == null) {
                return Mono.error(new ChatbotException("상품 선택 정보를 확인해 주세요.",
                        ChatbotErrorType.INVALID_CHAT_REQUEST));
            }
            return sessionRepository.findChatSession(memberId)
                    .timeout(sessionReadTimeout)
                    .switchIfEmpty(Mono.error(new ChatbotException("상담 내역을 찾을 수 없습니다.",
                            ChatbotErrorType.CHAT_SESSION_READ_FAILED)))
                    .flatMap(session -> {
                        if (!Objects.equals(memberId, session.getMemberId())) {
                            return Mono.error(new ChatbotException("상담 정보를 확인할 수 없습니다.",
                                    ChatbotErrorType.INVALID_CHAT_REQUEST));
                        }

                        // 현재 Redis Session에 저장이 되어 있는지 확인
                        List<UserRequestProductDto> candidates = session.getCandidates() == null
                                ? List.of() : session.getCandidates();

                        UserRequestProductDto selected = candidates.stream()
                                .filter(Objects::nonNull)
                                .filter(candidate -> variantId.equals(candidate.getProductVariantId()))
                                .findFirst().orElseThrow(() -> new ChatbotException(
                                        "현재 후보에 없는 상품입니다.", ChatbotErrorType.INVALID_CHAT_REQUEST));
                        ChatSessionDto confirmed = new ChatSessionDto(memberId, selected,
                                List.of(), session.getMessages());

                        return sessionRepository.save(memberId, confirmed)
                                .timeout(sessionWriteTimeout)
                                .filter(Boolean.TRUE::equals)
                                .switchIfEmpty(Mono.error(new ChatbotException("상품 확정 저장에 실패했습니다.",
                                        ChatbotErrorType.CHAT_SESSION_READ_FAILED)))
                                .thenReturn(ProductSearchResultRecord.found("CONFIRMED", List.of(
                                        new ProductSearchResultDto(selected.getProductVariantId(), selected.getProductName()))));
                    });
        }).onErrorMap(error -> error instanceof ChatbotException ? error :
                new ChatbotException("상품 확정 처리에 실패했습니다.", ChatbotErrorType.CHAT_SESSION_READ_FAILED));
    }


//    @Tool(description = "확정된 productVariantId로 상품명, 설명, 색상과 판매 상태를 조회한다. 사용자가 언급하지 않았거나 검증되지 않은 ID를 임의로 생성하지 않는다.")
//    public void getProductDetails(Long productVariantId) {
//
//    }
//
//    @Tool(description = "확정된 productVariantId의 실제 사이즈 옵션과 옵션별 판매 가능 여부를 조회한다. " +
//            "리뷰에 등장한 사이즈가 현재 판매 옵션에도 존재하는지 확인할 때 사용한다.")
//    public void getSizeOptions(Long productVariantId) {
//
//    }
//
//    @Tool(description = "확정된 productVariantId의 사이즈별 실측표와 측정 단위를 조회한다. 총장, 가슴, 어깨 등 실제 등록된 측정값만 반환하며 실측표가 없으면 값을 추정하지 않는다.")
//    public void getSizeChart(Long productVariantId) {
//
//    }
//
//    @Tool(description = "확정된 productVariantId 안에서 사용자 신체 조건과 착용 요구에 맞는 제한된 리뷰 근거를 검색한다. 신체정보는 선택값이며 의미 검색 문구에 키·몸무게를 포함하지 않는다. 검색 방식은 서버가 허용한 범위에서 선택하고 조건을 완화해도 원래 사용자 조건은 변경하지 않는다.")
//    public void searchReviews(Long productVariantId, Integer heightCm, Integer weightKg,
//                              String fitPreference, String query, String searchMode) {
//
//    }
//
//    @Tool(description = "확정된 productVariantId의 사이즈별 구매 건수와 착용감 분포, 표본 수를 조회한다. 사용자 조건은 선택적으로 적용하며 적용 조건과 집계 범위를 함께 확인한다. 제한된 표본을 전체 구매자의 통계나 개인 맞춤 결과로 해석하지 않는다.")
//    public void getReviewStatistics(Long productVariantId, Integer heightCm, Integer weightKg,
//                                    String fitPreference) {
//
//    }
//
//    @Tool(description = "이전 답변에서 인용한 리뷰 ID 목록으로 해당 근거를 다시 조회한다. 확정된 productVariantId에 속하고 삭제·제외되지 않은 리뷰만 대상으로 하며 조회할 수 없는 근거를 이전 답변만으로 대체하지 않는다.")
//    public void getReviewEvidenceByIds(Long productVariantId, List<Long> reviewIds) {
//
//    }
//
//    @Tool(description = "같은 확정 productVariantId의 두 실제 사이즈에 대한 리뷰 근거를 비교한다. 사용자 조건별 표본 수, 착용 경향과 상충 근거를 구분하고 근거 부족 시 어느 사이즈가 적합한지 단정하지 않는다. 서로 다른 상품 비교에는 사용하지 않는다.")
//    public void compareSizeEvidence(Long productVariantId, String firstSize, String secondSize,
//                                    Integer heightCm, Integer weightKg, String fitPreference) {
//
//    }
}
