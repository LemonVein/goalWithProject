package com.jason.goalwithproject.dto.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TokenResponseWithStatus {
    private String accessToken;
    private String refreshToken;
    private String status;
}
