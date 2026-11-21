package com.sh.commercechatbot.gpt.dto.request;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sh.commercechatbot.gpt.dto.response.GptToolCall;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GptRequestCompletionDto {

    private GptMessageRole role;
    private String content;

    @JsonProperty("tool_calls")
    private List<GptToolCall> toolCalls;
    @JsonProperty("tool_call_id")
    private String toolCallId;

    public GptRequestCompletionDto(GptMessageRole role, String content) {
        this(role, content, null, null);
    }
}