package com.sh.commercechatbot.llm.dto.request;

import com.sh.commercechatbot.chat.dto.request.ChatMessageDto;
import com.sh.commercechatbot.llm.enums.LlmModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LlmChatRequestDto {
    private String request;
    private String prompt;
    private boolean useJson;
    private LlmModel llmModel;
    private List<ChatMessageDto> messageList;


    public LlmChatRequestDto(String request, String systemPrompt, LlmModel model) {
        this.request = request;
        this.prompt = systemPrompt;
        this.useJson = true;
        this.llmModel = model;
    }
}