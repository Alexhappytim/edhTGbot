package com.alexhappytim.edhTGbot.backend.controller;

import com.alexhappytim.mtg.dto.*;
import com.alexhappytim.edhTGbot.backend.model.User;
import com.alexhappytim.edhTGbot.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<UserDTO> registerUser(@RequestBody CreateUserRequest request) {
        if (request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new IllegalArgumentException("Display name is required");
        }

        if (request.getUserTag() == null || request.getUserTag().isBlank()) {
            throw new IllegalArgumentException("UserTag is required");
        }
        if (request.getTelegramId() == null) {
            throw new IllegalArgumentException("TelegramId is required");
        }
        if (request.getChatId() == null) {
            throw new IllegalArgumentException("ChatId is required");
        }
        if (userRepository.findByUserTag(request.getUserTag()).isPresent()) {
            throw new IllegalStateException("UserTag already exists");
        }




        User user = User.builder()
                .userTag(request.getUserTag())
                .displayName(request.getDisplayName())
                .telegramId(request.getTelegramId())
                .chatId(request.getChatId())
                .build();
        user = userRepository.save(user);
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUserTag(user.getUserTag());
        dto.setTelegramId(user.getTelegramId());
        dto.setDisplayName(user.getDisplayName());
        dto.setChatId(user.getChatId());
        return ResponseEntity.ok(dto);
    }
}
