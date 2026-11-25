package com.sh.commercechatbot.es.review.dto;

import com.sh.commercechatbot.es.review.document.ReviewDocument;
import com.sh.commercechatbot.enums.review.FitEvaluation;
import lombok.Getter;

/** 임베딩·신체정보를 제외한 제한된 리뷰 근거. */
@Getter
public class ReviewEvidenceDto {
    private final Long reviewId;
    private final Long productVariantId;
    private final String purchasedSize;
    private final FitEvaluation fitEvaluation;
    private final String content;

    public ReviewEvidenceDto(ReviewDocument document) {
        this.reviewId = document.getReviewId();
        this.productVariantId = document.getProductVariantId();
        this.purchasedSize = document.getPurchasedSize();
        this.fitEvaluation = document.getFitEvaluation();
        String text = document.getContent();
        this.content = text == null ? null : text.substring(0, Math.min(text.length(), 500));
    }
}
