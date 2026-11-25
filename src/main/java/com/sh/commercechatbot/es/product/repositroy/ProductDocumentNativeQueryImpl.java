package com.sh.commercechatbot.es.product.repositroy;

import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.sh.commercechatbot.es.product.document.ProductDocument;
import com.sh.commercechatbot.es.product.dto.ProductSearchResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@Slf4j
public class ProductDocumentNativeQueryImpl implements ProductDocumentNativeQuery{

    private final ReactiveElasticsearchOperations operations;

    @Override
    public Flux<ProductSearchResultDto> search(String keyword) {


        NativeQuery query = NativeQuery.builder()
                // Query 설정
                .withQuery(q -> q
                                .bool(b -> b
                                                .must(m -> m
                                                        .multiMatch(mm -> mm
                                                                .query(keyword)
                                                                .fields(
                                                                        "variantName",
                                                                        "baseProductName",
                                                                        "brandName"
                                                                )
                                                                .type(TextQueryType.CrossFields)
                                                                // 모든 검색 토큰이 일치해야 하므로 '후드'만 같은 다른 상품은 제외한다.
                                                                .operator(Operator.And)
                                                        )
                                                )
                                )
                )
                // 페이징 설정
                .withPageable(PageRequest.of(0, 10))
                .build();

        return operations.search(query, ProductDocument.class)
                .map(hits -> {
                    ProductSearchResultDto searchResultDto = new ProductSearchResultDto(hits.getContent());
                    return searchResultDto;
                });

    }
}
