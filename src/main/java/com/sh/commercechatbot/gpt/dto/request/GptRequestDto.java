package com.sh.commercechatbot.gpt.dto.request;

import com.sh.commercechatbot.gpt.dto.response.GptResponseFormat;
import com.sh.commercechatbot.chat.dto.request.ChatMessageDto;
import com.sh.commercechatbot.llm.dto.request.LlmChatRequestDto;
import com.sh.commercechatbot.llm.enums.LlmModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GptRequestDto {
    private List<GptRequestCompletionDto> messages;
    private LlmModel model;
    private Boolean stream;
    private Integer max_completion_tokens = 1024;
    private GptResponseFormat response_format;

    public GptRequestDto(LlmChatRequestDto llmChatRequestDto, Boolean isStream) {
        this.messages = new ArrayList<>();
        this.messages.add(new GptRequestCompletionDto(GptMessageRole.SYSTEM, llmChatRequestDto.getPrompt()));
        if (llmChatRequestDto.getMessageList() == null) {
            this.messages.add(new GptRequestCompletionDto(GptMessageRole.USER, llmChatRequestDto.getRequest()));
        } else {
            for (ChatMessageDto message : llmChatRequestDto.getMessageList()) {
                GptMessageRole role = switch (message.getRole()) {
                    case "USER" -> GptMessageRole.USER;
                    case "ASSISTANT" -> GptMessageRole.ASSISTANT;
                    default -> throw new IllegalArgumentException("Invalid chat message role");
                };
                this.messages.add(new GptRequestCompletionDto(role, message.getContent()));
            }
        }
        this.stream = isStream;
        this.model = llmChatRequestDto.getLlmModel();
    }
}