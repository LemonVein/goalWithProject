package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.dto.custom.CharacterDto;
import com.jason.goalwithproject.dto.jwt.RefreshTokenDto;
import com.jason.goalwithproject.dto.jwt.TokenResponse;
import com.jason.goalwithproject.dto.jwt.TokenResponseWithStatus;
import com.jason.goalwithproject.dto.user.UserDto;
import com.jason.goalwithproject.dto.user.UserLoginDto;
import com.jason.goalwithproject.dto.user.UserRegisterDto;
import com.jason.goalwithproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.Map;


@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> TryLogin(@RequestBody UserLoginDto userLoginDto) {
        TokenResponse tokens = userService.TryLogin(userLoginDto);
        return ResponseEntity.ok(tokens);
    }

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<TokenResponseWithStatus> signUp(@RequestBody UserRegisterDto userRegisterDto) {
        TokenResponseWithStatus tokens = userService.TrySignUp(userRegisterDto);
        return ResponseEntity.ok(tokens);
    }

    // 유저 정보 확인
    @GetMapping("/info")
    public ResponseEntity<UserDto> GetUserInfo(@RequestHeader("Authorization") String authorization) {
        UserDto dto = userService.getUserInfo(authorization);
        return ResponseEntity.ok(dto);
    }

    // 유저가 가지고 있는 캐릭터들 조회
    @GetMapping("/characters/{userId}")
    public ResponseEntity<Page<CharacterDto>> GetCharacters(@RequestHeader("Authorization") String authorization, @PathVariable("userId") Long userId, Pageable pageable) throws AccessDeniedException {
        Page<CharacterDto> dtos = userService.getCharacters(authorization, userId, pageable);
        return ResponseEntity.ok(dtos);
    }


    // refresh 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshRefreshToken(@RequestBody RefreshTokenDto refreshTokenDto) {
        TokenResponse token = userService.reissueToken(refreshTokenDto.getRefreshToken());
        return ResponseEntity.ok(token);
    }

}
