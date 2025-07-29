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
                "userId",           claims.get("userId", String.class),
                "email",        claims.get("email", String.class),
                "nickname",     claims.get("nickname", String.class),
                "userType",     claims.get("userType", String.class),
                "level",        claims.get("level", Integer.class),
                "actionPoints", claims.get("actionPoints", Integer.class)
        );

        return jwtTokenProvider.generateAccessToken(claimsMap);
    }

    public String parseRefreshToken(String token) {
        Claims claims = jwtTokenProvider.parseToken(token);

        Map<String, Object> claimsMap = Map.of(
                "userId",           claims.get("userId", String.class),
                "email",        claims.get("email", String.class),
                "nickname",     claims.get("nickname", String.class),
                "userType",     claims.get("userType", String.class),
                "level",        claims.get("level", Integer.class),
                "actionPoints", claims.get("actionPoints", Integer.class)
        );

        return jwtTokenProvider.generateRefreshToken(claimsMap);
    }

    public Claims extractClaimsFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.substring("Bearer ".length());

        try {
            return jwtTokenProvider.parseToken(token);
        } catch (Exception e) {
            return null;
        }
    }
}
