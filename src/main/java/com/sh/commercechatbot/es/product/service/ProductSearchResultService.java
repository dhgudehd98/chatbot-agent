package com.sh.commercechatbot.es.product.service;

import com.sh.commercechatbot.chat.dto.request.ChatRequestDto;
import com.sh.commercechatbot.es.product.dto.ProductSearchResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchResultService {


    public void productNotFound(Long memberId, ChatRequestDto chatRequestDto) {


    }

    public void productSingleFound(Long memberId, ChatRequestDto requestDto, ProductSearchResultDto resultDto) {
    }

    public void productMultiFound(Long memberId, ChatRequestDto chatRequestDto,List<ProductSearchResultDto> searchResults) {
    }
}