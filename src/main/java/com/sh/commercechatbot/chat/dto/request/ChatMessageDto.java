package com.sh.commercechatbot.chat.dto.request;

import java.util.List;
import com.sh.commercechatbot.gpt.dto.response.GptToolCall;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private String role; // USER || ASSISTANT || TOOL
    private String content; // 사용자의 요청이나 LLM의 답변에 대한 내용

    private List<GptToolCall> toolCalls;
    private String toolCallId;

    public ChatMessageDto(String role, String content) {
        this(role, content, null, null);
    }
}