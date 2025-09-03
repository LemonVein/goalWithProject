package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtTokenProvider jwtTokenProvider;

    public String parseAccessToken(String token) {
        Claims claims = jwtTokenProvider.parseToken(token);

        Map<String, Object> claimsMap = Map.of(
                "userId",           claims.get("userId", String.class)
        );

        return jwtTokenProvider.generateAccessToken(claimsMap);
    }

    public String parseRefreshToken(String token) {
        Claims claims = jwtTokenProvider.parseToken(token);

        Map<String, Object> claimsMap = Map.of(
                "userId",           claims.get("userId", String.class)
        );

        return jwtTokenProvider.generateRefreshToken(claimsMap);
    }

    public Claims extractClaimsFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();

        try {
            return jwtTokenProvider.parseToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    public Long UserIdFromToken(String authorizationHeader) {
        Claims claims = extractClaimsFromAuthorizationHeader(authorizationHeader);
        if (claims == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            throw new IllegalArgumentException("토큰에 사용자 ID가 없습니다.");
        }

        if (userIdObj instanceof Number) {
            return ((Number) userIdObj).longValue();
        } else {
            try {
                return Long.valueOf(userIdObj.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("토큰의 사용자 ID가 유효한 숫자가 아닙니다.");
            }
        }
    }
}
