package com.sh.commercechatbot.llm.dto.request;

import com.sh.commercechatbot.chat.dto.request.ChatMessageDto;
import com.sh.commercechatbot.gpt.dto.request.GptToolDefinition;
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
    private List<GptToolDefinition> tools;

    public  LlmChatRequestDto(String request, String prompt, boolean useJson,
                             LlmModel model, List<ChatMessageDto> messageList) {
        this(request, prompt, useJson, model, messageList, null);
    }

    public LlmChatRequestDto withPrompt(String prompt) {
        return new LlmChatRequestDto(request, prompt, useJson, llmModel, messageList, tools);
    }

    public LlmChatRequestDto withTools(List<GptToolDefinition> tools) {
        return new LlmChatRequestDto(request, prompt, useJson, llmModel, messageList, List.copyOf(tools));
    }

}