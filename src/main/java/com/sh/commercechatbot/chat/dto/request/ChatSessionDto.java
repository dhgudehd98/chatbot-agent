package com.sh.commercechatbot.chat.dto.request;





import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionDto {
    private Long memberId;
    private UserRequestProductDto productDto;
    private List<ChatMessageDto> messages;

    public static ChatSessionDto create(Long memberId) {
        return new ChatSessionDto(memberId, null, List.of());
    }

    
}