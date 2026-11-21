package com.sh.commercechatbot.chat.dto.request;

import com.sh.commercechatbot.enums.chatbot.request.ChatType;
import com.sh.commercechatbot.llm.enums.LlmModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequestDto {
    private Long selectedVariantId;
    private ChatType chatType;
    private String currentMessage;
    private LlmModel llmModel;

    @Override
    public String toString() {
        return "사용자 요청 정보 {" +
                "선택 상품(VariantId) : " + selectedVariantId == null ? "상품 선택 안함" : selectedVariantId +
                ", 사용자 채팅 입력 값 : '" + currentMessage != null ? currentMessage : "사용자 입력 값 없음." + '\'' +
                '}';
    }
}