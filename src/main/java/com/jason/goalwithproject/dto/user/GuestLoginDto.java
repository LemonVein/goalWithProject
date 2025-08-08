package com.jason.goalwithproject.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestLoginDto {
    private String name;
    private String email;
    private String nickname;
    private String userType;
    private String password;
}
