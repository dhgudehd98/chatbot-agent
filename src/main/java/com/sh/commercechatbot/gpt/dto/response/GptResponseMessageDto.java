package com.sh.commercechatbot.gpt.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class GptResponseMessageDto {
    private String content;
    private List<GptToolCall> tool_calls;
}