package com.jason.goalwithproject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jason.goalwithproject.dto.jwt.AppleInfo;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppleAuthService {
    private final ObjectMapper objectMapper;

    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";

    public String getAppleUserId(String identityToken) {
        try {
            // 프론트에서 받은 토큰 파싱
            SignedJWT signedJWT = SignedJWT.parse(identityToken);

            // 애플의 공개키 목록 가져오기
            RestTemplate restTemplate = new RestTemplate();
            String publicKeysResponse = restTemplate.getForObject(APPLE_PUBLIC_KEYS_URL, String.class);

            // 토큰 헤더의 Key ID (kid) 찾기
            String kid = signedJWT.getHeader().getKeyID();

            // 공개키 목록에서 내 토큰과 맞는 키 찾기
            Map<String, Object> keysMap = objectMapper.readValue(publicKeysResponse, Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) keysMap.get("keys");

            Map<String, Object> matchingKey = keys.stream()
                    .filter(key -> key.get("kid").equals(kid))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("일치하는 애플 공개키를 찾을 수 없습니다."));

            // 검증 가능한 RSA 공개키 객체로 변환
            RSAKey rsaKey = RSAKey.parse(matchingKey);
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            JWSVerifier verifier = new RSASSAVerifier(publicKey);

            // 서명 검증 수행
            if (!signedJWT.verify(verifier)) {
                throw new IllegalArgumentException("유효하지 않은 애플 토큰 서명입니다.");
            }

            // 클레임(정보) 확인
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // 애플 고유 ID (sub) 반환
            return claims.getSubject();

        } catch (ParseException | JOSEException | JsonProcessingException e) {
            throw new RuntimeException("애플 로그인 검증 실패: " + e.getMessage());
        }
    }
    public AppleInfo getAppleUserInfo(String identityToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(identityToken);
            // 애플의 공개키 목록 가져오기
            RestTemplate restTemplate = new RestTemplate();
            String publicKeysResponse = restTemplate.getForObject(APPLE_PUBLIC_KEYS_URL, String.class);

            // 토큰 헤더의 Key ID (kid) 찾기
            String kid = signedJWT.getHeader().getKeyID();

            // 공개키 목록에서 내 토큰과 맞는 키 찾기
            Map<String, Object> keysMap = objectMapper.readValue(publicKeysResponse, Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) keysMap.get("keys");

            Map<String, Object> matchingKey = keys.stream()
                    .filter(key -> key.get("kid").equals(kid))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("일치하는 애플 공개키를 찾을 수 없습니다."));

            // 검증 가능한 RSA 공개키 객체로 변환
            RSAKey rsaKey = RSAKey.parse(matchingKey);
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            JWSVerifier verifier = new RSASSAVerifier(publicKey);

            // 서명 검증 수행
            if (!signedJWT.verify(verifier)) {
                throw new IllegalArgumentException("유효하지 않은 애플 토큰 서명입니다.");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            String sub = claims.getSubject();

            // 이메일 클레임 꺼내기
            String email = claims.getStringClaim("email");

            return new AppleInfo(sub, email);

        } catch (Exception e) {
            throw new RuntimeException("애플 토큰 검증 실패", e);
        }
    }

}
