package com.sh.commercechatbot.es.product.repositroy;

import com.sh.commercechatbot.es.product.dto.ProductSearchResultDto;
import reactor.core.publisher.Flux;

public interface ProductDocumentNativeQuery {
    Flux<ProductSearchResultDto> search(String keyword);
}
