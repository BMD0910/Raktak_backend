package com.raktakk.backend.service;

import com.raktakk.backend.dto.UserMeResponse;
import com.raktakk.backend.dto.UserStatusResponse;
import com.raktakk.backend.entity.User;
import com.raktakk.backend.entity.AccountStatus;
import com.raktakk.backend.exception.ResourceNotFoundException;
import com.raktakk.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserMeResponse toMe(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getAccountStatus() == null ? "active" : user.getAccountStatus().name().toLowerCase(),
                user.getDeactivationReason(),
                user.getDeactivationContact()
        );
    }

    public UserStatusResponse toStatus(User user) {
        return UserStatusResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus() != null ? user.getAccountStatus().name().toLowerCase() : AccountStatus.ACTIVE.name().toLowerCase())
                .deactivationReason(user.getDeactivationReason())
                .deactivationContact(user.getDeactivationContact())
                .deactivatedAt(user.getDeactivatedAt())
                .deactivatedBy(user.getDeactivatedBy())
                .build();
    }

    public Page<UserMeResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toMe);
    }
}
