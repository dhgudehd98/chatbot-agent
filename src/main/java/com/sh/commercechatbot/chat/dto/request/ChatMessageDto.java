package com.sh.commercechatbot.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private String role; // USER || ASSISTANT
    private String content; // 사용자의 요청이나 LLM의 답변에 대한 내용
}