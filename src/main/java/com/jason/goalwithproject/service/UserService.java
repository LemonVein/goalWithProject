package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.JwtTokenProvider;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.jwt.TokenResponse;
import com.jason.goalwithproject.dto.jwt.TokenResponseWithStatus;
import com.jason.goalwithproject.dto.peer.RequesterDto;
import com.jason.goalwithproject.dto.user.UserDto;
import com.jason.goalwithproject.dto.user.UserLoginDto;
import com.jason.goalwithproject.dto.user.UserRegisterDto;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserTypeRepository userTypeRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final DtoConverterService dtoConverterService;
    private final JwtService jwtService;

    @Transactional
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
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(jwtTokenProvider.getREFRESH_EXPIRATION_TIME() / 1000);

        userRefreshTokenRepository.findByUser_Id(user.getId())
                .ifPresentOrElse(userRefreshToken -> userRefreshToken.updateToken(refreshToken, expiryTime),
                        () -> userRefreshTokenRepository.save(new UserRefreshToken(user, refreshToken, expiryTime)));

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

    @Transactional
    public TokenResponse reissueToken(String refreshToken) {
        Optional<UserRefreshToken> userRefreshToken = userRefreshTokenRepository.findByToken(refreshToken);
        if (userRefreshToken.isEmpty()) {
            throw new IllegalArgumentException("리프레쉬 토큰이 만료되었습니다");
        }

        UserRefreshToken userRefreshTokenEntity = userRefreshToken.get();
        User targetUser = userRefreshTokenEntity.getUser();

        Map<String, Object> claims = Map.of("userId", targetUser.getId());
        String newAccessToken = jwtTokenProvider.generateAccessToken(claims);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(claims);
        LocalDateTime newExpiryTime = LocalDateTime.now().plusSeconds(jwtTokenProvider.getREFRESH_EXPIRATION_TIME() / 1000);

        userRefreshTokenEntity.updateToken(newRefreshToken, newExpiryTime);

        return new TokenResponse(newAccessToken, newRefreshToken);

    }

    // 이름으로 유저들을 검색하는 메서드
    @Transactional(readOnly = true)
    public Page<RequesterDto> searchUsers(String authorization, String keyword, Pageable pageable) {
        Long currentUserId = jwtService.UserIdFromToken(authorization);

        Page<User> userPage = userRepository.findByNameContaining(keyword, pageable);

        return userPage.map(user -> {
            // ★★★ 검색 결과에서 자기 자신은 제외합니다 ★★★
            if (user.getId().equals(currentUserId)) {
                return null; // null을 반환하면 최종 결과에서 자동으로 제외됩니다.
            }

            // 캐릭터 정보를 조회합니다.
            UserCharacter userCharacter = userCharacterRepository.findByUser_Id(user.getId());
            String characterImageUrl = (userCharacter != null && userCharacter.getCharacterImage() != null)
                    ? userCharacter.getCharacterImage().getImage()
                    : null;

            return RequesterDto.builder()
                    .id(user.getId())
                    .name(user.getNickName())
                    .character(characterImageUrl)
                    .userType(user.getUserType().getName())
                    .level(user.getLevel())
                    .build();
        });
    }
}
