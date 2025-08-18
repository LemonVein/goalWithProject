package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.JwtTokenProvider;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.jwt.TokenResponse;
import com.jason.goalwithproject.dto.jwt.TokenResponseWithStatus;
import com.jason.goalwithproject.dto.user.UserDto;
import com.jason.goalwithproject.dto.user.UserLoginDto;
import com.jason.goalwithproject.dto.user.UserRegisterDto;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserTypeRepository userTypeRepository;
    private final DtoConverterService dtoConverterService;
    private final JwtService jwtService;

    public TokenResponse TryLogin(UserLoginDto userLoginDto) {
        User user = userRepository.findByEmail(userLoginDto.getEmail()).orElse(null);
        if (user == null) {
            return null;
        }

        if (!passwordEncoder.matches(userLoginDto.getPassword(), user.getPassword())) {
            return null;
        }

        Map<String, Object> claims = Map.of(
                "userId", user.getId()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(claims);

        TokenResponse response = new TokenResponse(accessToken, refreshToken);
        return response;
    }

    public TokenResponseWithStatus TrySignUp(UserRegisterDto userRegisterDto) {

        if (userRepository.existsByEmail(userRegisterDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickName(userRegisterDto.getNickName())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        String hashed = passwordEncoder.encode(userRegisterDto.getPassword());
        UserType userType = userTypeRepository.findByName(userRegisterDto.getUserType());
        User user = new User(
                userRegisterDto.getName(),
                userRegisterDto.getEmail(),
                hashed,
                userRegisterDto.getNickName(),
                userType
        );

        User saveUser = userRepository.save(user);
        Map<String, Object> claims = Map.of(
                "userId", saveUser.getId()
        );
        String accessToken = jwtTokenProvider.generateAccessToken(claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(claims);

        return new TokenResponseWithStatus(accessToken, refreshToken, "success");
    }

    public UserDto getUserInfo(String authorization) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        UserDto dto = dtoConverterService.convertToDto(user);
        return dto;

    }
}
