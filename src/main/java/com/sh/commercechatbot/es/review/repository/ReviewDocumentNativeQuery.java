package com.sh.commercechatbot.es.review.repository;


import com.sh.commercechatbot.es.review.document.ReviewDocument;
import com.sh.commercechatbot.es.review.dto.ReviewOptionalInfoDto;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ReviewDocumentNativeQuery {

    Mono<List<ReviewDocument>> searchReviewAll(Long variantId, String preferenceQuery);

    Mono<List<ReviewDocument>> searchReviewByBodyInfoAndPreferenceQuery(Long variantId, Integer weight, Integer height, String preferenceQuery);

}
