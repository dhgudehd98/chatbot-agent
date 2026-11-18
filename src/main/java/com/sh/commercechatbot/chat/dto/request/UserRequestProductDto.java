package com.sh.commercechatbot.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestProductDto {
    private Long productVariantId;
    private String productName;
}