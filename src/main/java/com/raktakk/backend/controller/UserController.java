package com.raktakk.backend.controller;

import com.raktakk.backend.dto.UserMeResponse;
import com.raktakk.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserMeResponse me(org.springframework.security.core.Authentication authentication) {
        return userService.toMe(userService.getByEmail(authentication.getName()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserMeResponse> adminUsers(Pageable pageable) {
        return userService.listUsers(pageable);
    }
}
