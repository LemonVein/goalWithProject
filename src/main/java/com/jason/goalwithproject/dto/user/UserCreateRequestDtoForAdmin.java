package com.jason.goalwithproject.dto.user;

import lombok.Getter;

@Getter
public class UserCreateRequestDtoForAdmin {
    private String nickname;
    private String userType;
    private int level;
    private int actionPoints;
}
