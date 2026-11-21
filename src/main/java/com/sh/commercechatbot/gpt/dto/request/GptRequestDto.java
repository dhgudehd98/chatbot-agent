package com.sh.commercechatbot.gpt.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<GptToolDefinition> tools;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String tool_choice;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean parallel_tool_calls;

    public GptRequestDto(LlmChatRequestDto llmChatRequestDto, Boolean isStream) {

        this.messages = new ArrayList<>();
        this.messages.add(new GptRequestCompletionDto(GptMessageRole.SYSTEM, llmChatRequestDto.getPrompt()));

        // 기존의 대화 이력이 존재하지 않다면, PROMPT + 현재 요청에 대한 정보만 전달
        if (llmChatRequestDto.getMessageList() == null) {
            this.messages.add(new GptRequestCompletionDto(GptMessageRole.USER, llmChatRequestDto.getRequest()));
        }
        else {
            // 기존의 대화 내력 포함해서 전달
            for (ChatMessageDto message : llmChatRequestDto.getMessageList()) {
                GptMessageRole role = switch (message.getRole()) {
                    case "USER" -> GptMessageRole.USER;
                    case "ASSISTANT" -> GptMessageRole.ASSISTANT;
                    default -> throw new IllegalArgumentException("Invalid chat message role");
                };
                this.messages.add(new GptRequestCompletionDto(role, message.getContent()));
            }
        }

        if (!isStream && llmChatRequestDto.getTools() != null && !llmChatRequestDto.getTools().isEmpty()) {
            this.tools = List.copyOf(llmChatRequestDto.getTools());
            this.tool_choice = "auto";
            this.parallel_tool_calls = false;
        }
        this.stream = isStream;
        this.model = llmChatRequestDto.getLlmModel();
    }
}