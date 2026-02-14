package com.jason.goalwithproject.dto.user;

import com.jason.goalwithproject.domain.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserInfoForAdmin {
    private Long id;
    private String nickname;
    private String email;
    private String character;
    private int level;
    private int actionPoints;
    private String badge;
    private String userType;
    private LocalDateTime createdAt;
    private String userStatus;
}
