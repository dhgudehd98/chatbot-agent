package com.sh.commercechatbot.agent.review.tool.dto;

import com.sh.commercechatbot.es.product.dto.ProductSearchResultDto;
import com.sh.commercechatbot.es.product.dto.ProductSearchResultRecord;
import com.sh.commercechatbot.es.review.dto.ReviewEvidenceDto;
import java.util.List;

/** 상품과 리뷰 Tool 결과를 같은 실행 경로로 전달한다. */
public record ReviewToolResult(String status, List<ProductSearchResultDto> results,
                               List<ReviewEvidenceDto> reviews) {
    public static ReviewToolResult product(ProductSearchResultRecord result) {
        return new ReviewToolResult(result.status(), result.results(), List.of());
    }
    public static ReviewToolResult reviews(List<ReviewEvidenceDto> reviews) {
        return new ReviewToolResult(reviews.isEmpty() ? "INSUFFICIENT_REVIEW_DATA" : "REVIEWS_FOUND",
                List.of(), List.copyOf(reviews));
    }
}
