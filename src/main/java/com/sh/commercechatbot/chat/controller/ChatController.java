package com.sh.commercechatbot.chat.controller;

import com.sh.commercechatbot.chat.service.ChatService;
import com.sh.commercechatbot.chat.dto.request.ChatRequestDto;
import com.sh.commercechatbot.chat.dto.response.ChatResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.security.Principal;
import org.springframework.http.ResponseEntity;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("")
    public Mono<ResponseEntity<ChatResponseDto>> chat(
            @RequestBody ChatRequestDto request
//            Principal principal
    ) {
//        if (principal == null) {
//            return Mono.just(ResponseEntity.status(401).build());
//        }
//        try {
//            memberId = Long.parseLong(principal.getName());
//        } catch (NumberFormatException exception) {
//            return Mono.just(ResponseEntity.status(401).build());
//        }
//        if (memberId <= 0) {
//            return Mono.just(ResponseEntity.status(401).build());
//        }

        Long memberId = 1L;
        return chatService.chat(memberId, request).map(ResponseEntity::ok);
    }
}