package com.jason.goalwithproject.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserDto {
    private Long id;
    private String nickname;
    private String email;
    private int level;
    private int actionPoints;
    private int exp;
    private String userType;
    private String character;
    private String badge;
}
