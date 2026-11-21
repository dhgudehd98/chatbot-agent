package com.sh.commercechatbot.es.product.dto;

import java.util.List;

public record ProductSearchResultRecord(
        String status,
        List<ProductSearchResultDto> results
) {
    public static ProductSearchResultRecord notFound(String status) {
        return new ProductSearchResultRecord(status, List.of());
    }

    public static ProductSearchResultRecord found(String status, List<ProductSearchResultDto> results) {
        return new ProductSearchResultRecord(status, results);
    }
}
