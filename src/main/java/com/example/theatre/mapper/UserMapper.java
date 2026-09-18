package com.example.theatre.mapper;

import com.example.theatre.dto.UserResponse;
import com.example.theatre.dto.UserUpdateRequest;
import com.example.theatre.entity.User;

public final class UserMapper {
    private UserMapper() {}
    public static void update(User user, UserUpdateRequest request) {
        user.setName(request.name()); user.setEmail(request.email()); user.setPhone(request.phone());
    }
    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole(), user.getCreatedAt());
    }
}
