package com.sh.commercechatbot.chat.dto.request;





import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionDto {
    private Long memberId;
    private UserRequestProductDto productDto;
    private List<UserRequestProductDto> candidates;
    private List<ChatMessageDto> messages;

    public static ChatSessionDto create(Long memberId) {

        // 처음 생성하는 경우에는 사용자의 요청 값까지 저장
        return new ChatSessionDto(memberId, null, List.of(), List.of());
    }


}