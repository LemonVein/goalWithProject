package com.jason.goalwithproject.dto.jwt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GoogleAuthTokenResponse {
    private boolean isNewer;
    private String accessToken;
    private String refreshToken;
    private String email;
    private String name;
}
