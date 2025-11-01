package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.dto.custom.CharacterDto;
import com.jason.goalwithproject.dto.custom.CharacterIdDto;
import com.jason.goalwithproject.dto.jwt.*;
import com.jason.goalwithproject.dto.user.*;
import com.jason.goalwithproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.security.GeneralSecurityException;
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

    // 유저 정보 불러오기
    @GetMapping("/{userId}")
    public ResponseEntity<UserInformationDto> getUserInfo(@RequestHeader("Authorization") String authorization, @PathVariable Long userId) {
        UserInformationDto dto = userService.getUserInformation(authorization, userId);
        return ResponseEntity.ok(dto);
    }

    // 유저 정보 확인
    @GetMapping("/info")
    public ResponseEntity<UserDto> GetUserInfo(@RequestHeader("Authorization") String authorization) {
        UserDto dto = userService.getUserInfo(authorization);
        return ResponseEntity.ok(dto);
    }

    // 닉네임 및 유정 타입 수정
    @PutMapping("/info")
    public ResponseEntity<Void> editUserInfo(@RequestHeader("Authorization") String authorization, @RequestBody UserEditInfoDto userEditInfoDto) {
        userService.editUserInfo(authorization, userEditInfoDto);
        return ResponseEntity.noContent().build();
    }

    // 유저가 가지고 있는 캐릭터들 조회
    @GetMapping("/characters/{userId}")
    public ResponseEntity<Page<CharacterDto>> GetCharacters(@RequestHeader("Authorization") String authorization, @PathVariable("userId") Long userId,
                                                            @PageableDefault(size = 5) Pageable pageable) throws AccessDeniedException {
        Page<CharacterDto> dtos = userService.getCharacters(authorization, userId, pageable);
        return ResponseEntity.ok(dtos);
    }

    // 대표 캐릭터 변경하기
    @PutMapping("/character/{userId}")
    public ResponseEntity<Void> UpdateCharacter(@RequestHeader("Authorization") String authorization, @PathVariable("userId") Long userId, @RequestBody CharacterIdDto characterDto) throws AccessDeniedException {
        userService.updateCharacter(authorization, userId, characterDto);
        return ResponseEntity.noContent().build();
    }


    // refresh 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshRefreshToken(@RequestBody RefreshTokenDto refreshTokenDto) {
        TokenResponse token = userService.reissueToken(refreshTokenDto.getRefreshToken());
        return ResponseEntity.ok(token);
    }

    // 구글 로그인 시도 (여기서 토큰은 인증 코드)
    @PostMapping("/google-login")
    public ResponseEntity<GoogleAuthTokenResponse> tryLoginGoogle(@RequestBody GoogleTokenDto tokenDto) throws GeneralSecurityException, IOException {
        GoogleAuthTokenResponse response = userService.authenticateGoogle(tokenDto);
        return ResponseEntity.ok(response);
    }

    // 카카오 로그인 시도
    @PostMapping("/kakao-login")
    public ResponseEntity<GoogleAuthTokenResponse> tryLoginKakao(@RequestBody KakaoTokenDto tokenDto) throws GeneralSecurityException, IOException {
        GoogleAuthTokenResponse response = userService.authenticateKakao(tokenDto);
        return ResponseEntity.ok(response);
    }



}
