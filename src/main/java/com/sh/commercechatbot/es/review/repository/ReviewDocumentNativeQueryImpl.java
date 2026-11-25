package com.sh.commercechatbot.es.review.repository;

import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.sh.commercechatbot.es.review.document.ReviewDocument;
import com.sh.commercechatbot.es.review.dto.ReviewOptionalInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


@RequiredArgsConstructor
@Slf4j
public class ReviewDocumentNativeQueryImpl implements ReviewDocumentNativeQuery {

    private final ReactiveElasticsearchOperations operations;
    private final EmbeddingModel embeddingModel;

    @Override
    public Mono<List<ReviewDocument>> searchReviewAll(Long variantId, String preferenceQuery) {

        Query filterQuery = Query.of(query -> query.bool(bool -> {
            bool.filter(filter -> filter.term(term -> term
                    .field("productVariantId")
                    .value(variantId)
            ));

            return bool;
        }));

        if (preferenceQuery != null) {
           return Mono.fromCallable(() -> embeddingModel.embed(preferenceQuery))
                   .subscribeOn(Schedulers.boundedElastic())
                   .flatMap(embedding -> {
                       List<Float> vectors = new ArrayList<>();

                       for (float vector : embedding) {
                           vectors.add(vector);
                       }

                       KnnQuery knnQuery = KnnQuery.of(k -> k
                               .field("contentEmbedding")
                               .queryVector(vectors)
                               // 필터에 맞는 리뷰 중 유사한 리뷰를 최대 10건 반환한다.
                               .k(10)
                               .numCandidates(100)
                               .filter(filterQuery));

                       NativeQuery nativeQuery = NativeQuery.builder()
                               .withKnnQuery(knnQuery)
                               .withPageable(PageRequest.of(0 , 10))
                               .build();

                       return operations.search(nativeQuery, ReviewDocument.class)
                               .map(searchHit -> searchHit.getContent())
                               .collectList();
                   });
        }

        NativeQuery nativeQuery = NativeQuery.builder()
                .withFilter(filterQuery)
                .withPageable(PageRequest.of(0, 10))
                .build();

        return operations.search(nativeQuery, ReviewDocument.class)
                .map(searchHit -> searchHit.getContent())
                .collectList();
    }



    @Override
    public Mono<List<ReviewDocument>> searchReviewByBodyInfoAndPreferenceQuery(
            Long variantId,
            Integer weight,
            Integer height,
            String preferenceQuery)
    {
        return Mono.deferContextual(context -> {
            String requestId = context.getOrDefault("chatRequestId", "unassigned");
            String searchCode = preferenceQuery == null || preferenceQuery.isBlank()
                    ? "REVIEW_ES_FILTER_SEARCH" : "REVIEW_ES_KNN_SEARCH";
            log.info("requestId={} code={} limit={}", requestId, searchCode, 10);

            Function<NativeQuery, Mono<List<ReviewDocument>>> executeSearch = query ->
                    operations.search(query, ReviewDocument.class)
                            .map(SearchHit::getContent)
                            .collectList()
                            .doOnNext(documents -> log.info("requestId={} code=REVIEW_ES_COMPLETED count={}",
                                    requestId, documents.size()))
                            .doOnError(error -> {
                                // 예외 메시지에는 리뷰 원문·신체정보가 포함될 수 있어 출력하지 않는다.
                                // 매핑/날짜/Enum 변환 실패를 구분할 수 있도록 클래스와 위치만 기록한다.
                                Throwable cause = error;
                                for (int depth = 0; depth < 20 && cause.getCause() != null
                                        && cause.getCause() != cause; depth++) {
                                    cause = cause.getCause();
                                }
                                StackTraceElement[] frames = cause.getStackTrace();
                                String location = frames.length == 0 ? "unknown"
                                        : frames[0].getClassName() + "." + frames[0].getMethodName();
                                log.warn("requestId={} code=REVIEW_ES_FAILED errorType={} causeType={} causeLocation={}",
                                        requestId, error.getClass().getName(), cause.getClass().getName(), location);
                            });

            Query filterQuery = Query.of(query -> query.bool(bool -> {
                bool.filter(filter -> filter.term(term -> term
                        .field("productVariantId")
                        .value(variantId)
                ));

                if (height != null) {
                    bool.filter(filter -> filter.range(range -> range
                            .field("heightCm")
                            .gte(JsonData.of(height - 5))
                            .lte(JsonData.of(height + 5))
                    ));
                }

                if (weight != null) {
                    bool.filter(filter -> filter.range(range -> range
                            .field("weightKg")
                            .gte(JsonData.of(weight - 5))
                            .lte(JsonData.of(weight + 5))
                    ));
                }

                return bool;
            }));

            if (preferenceQuery == null || preferenceQuery.isBlank()) {
                NativeQuery query = NativeQuery.builder()
                        .withQuery(filterQuery)
                        .withPageable(PageRequest.of(0, 10))
                        .build();

                return executeSearch.apply(query);
            }

            return Mono.fromCallable(() -> embeddingModel.embed(preferenceQuery))
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnSuccess(embedding -> log.info("requestId={} code=QUERY_EMBEDDING_COMPLETED", requestId))
                    .doOnError(error -> log.warn("requestId={} code=QUERY_EMBEDDING_FAILED", requestId))
                    .flatMap(embedding -> {
                        List<Float> vectors = new ArrayList<>();

                        for (float value : embedding) {
                            vectors.add(value);
                        }

                        KnnQuery knnQuery = KnnQuery.of(k -> k
                                        .field("contentEmbedding")
                                        .queryVector(vectors)
                                        // 필터에 맞는 리뷰 중 유사한 리뷰를 최대 10건 반환한다.
                                        .k(10)
                                        .numCandidates(100)
                                        .filter(filterQuery));

                        NativeQuery nativeQuery = NativeQuery.builder()
                                .withKnnQuery(knnQuery)
                                .withPageable(PageRequest.of(0 , 10))
                                .build();

                        return executeSearch.apply(nativeQuery);
                    });


        });
    }

}
