package com.piania.auth.mapper;

import com.piania.auth.dto.UserResponse;
import com.piania.auth.entity.User;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        String avatar = user.getAvatar() != null ? user.getAvatar().name() : User.Avatar.AVATAR_1.name();
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                avatar,
                user.getRole(),
                user.isPremium(),
                user.isChatNotificationsEnabled()
        );
    }
}
