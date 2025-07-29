package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.JwtTokenProvider;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.jwt.TokenResponse;
import com.jason.goalwithproject.dto.jwt.TokenResponseWithStatus;
import com.jason.goalwithproject.dto.user.UserLoginDto;
import com.jason.goalwithproject.dto.user.UserRegisterDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
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

    public TokenResponse TryLogin(UserLoginDto userLoginDto) {
        User user = userRepository.findByEmail(userLoginDto.getEmail()).orElse(null);
        if (user == null) {
            return null;
        }

        if (!passwordEncoder.matches(userLoginDto.getPassword(), user.getPassword())) {
            return null;
        }

        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "nickname", user.getNickName(),
                "userType", user.getUserType().getName(),
                "level", user.getLevel(),
                "actionPoints", user.getActionPoint()
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
                "userId", saveUser.getId(),
                "email", saveUser.getEmail(),
                "nickname", saveUser.getNickName(),
                "userType", saveUser.getUserType().getName(),
                "level", saveUser.getLevel(),
                "actionPoints", saveUser.getActionPoint()
        );
        String accessToken = jwtTokenProvider.generateAccessToken(claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(claims);

        return new TokenResponseWithStatus(accessToken, refreshToken, "success");
    }
}
