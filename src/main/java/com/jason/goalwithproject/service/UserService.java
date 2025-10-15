package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.JwtTokenProvider;
import com.jason.goalwithproject.domain.custom.BadgeRepository;
import com.jason.goalwithproject.domain.custom.CharacterImageRepository;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.custom.CharacterDto;
import com.jason.goalwithproject.dto.custom.CharacterIdDto;
import com.jason.goalwithproject.dto.jwt.TokenResponse;
import com.jason.goalwithproject.dto.jwt.TokenResponseWithStatus;
import com.jason.goalwithproject.dto.peer.RequesterDto;
import com.jason.goalwithproject.dto.user.*;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final CharacterImageRepository characterImageRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final DtoConverterService dtoConverterService;
    private final BadgeRepository badgeRepository;
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

        // 유저 기본 캐릭터 설정. 1번 캐릭터 기본 피코
        UserCharacter userCharacter = new UserCharacter();
        userCharacter.setUser(user);
        userCharacter.setCharacterImage(characterImageRepository.findById(1));
        userCharacter.setEquipped(true);
        userCharacterRepository.save(userCharacter);

        // 유저 기본 뱃지 설정. 1번 뱃지
        UserBadge userBadge = new UserBadge();
        userBadge.setUser(user);
        userBadge.setBadge(badgeRepository.findById(1).get());
        userBadgeRepository.save(userBadge);

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

    // 유저가 가진 캐릭터들을 리턴하는 메서드
    @Transactional(readOnly = true)
    public Page<CharacterDto> getCharacters(String authorization, Long userId, Pageable pageable) throws AccessDeniedException {
        Long authUserId = jwtService.UserIdFromToken(authorization);

        if (!Objects.equals(authUserId, userId)) {
            throw new AccessDeniedException("캐릭터를 불러올 권한이 없습니다.");
        }

        Page<UserCharacter> userCharacters = userCharacterRepository.findAllByUser_Id(userId, pageable);

        Page<CharacterDto> dtos = userCharacters.map(CharacterDto::new);

        return dtos;

    }

    // 유저의 대표 캐릭터를 변경
    @Transactional
    public void updateCharacter(String authorization, Long userId, CharacterIdDto characterIdDto) throws AccessDeniedException {
        Long myId = jwtService.UserIdFromToken(authorization);
        if (!Objects.equals(myId, userId)) {
            throw new AccessDeniedException("캐릭터를 변경할 권한이 없습니다.");
        }

        Long userCharacterIdToEquip = characterIdDto.getId();

        UserCharacter newEquippedCharacter = userCharacterRepository.findById(userCharacterIdToEquip)
                .orElseThrow(() -> new EntityNotFoundException("해당 캐릭터를 소유하고 있지 않습니다."));

        if (!newEquippedCharacter.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("자신이 소유한 캐릭터만 장착할 수 있습니다.");
        }

        userCharacterRepository.findByUser_IdAndIsEquippedTrue(userId).ifPresent(oldEquippedCharacter -> {
            oldEquippedCharacter.setEquipped(false);
        });

        newEquippedCharacter.setEquipped(true);
    }

    // 수정 필요
    @Transactional
    public void editUserInfo(String authorization, UserEditInfoDto userEditInfoDto) {
        Long currentUserId = jwtService.UserIdFromToken(authorization);

        User user = userRepository.findById(currentUserId).orElse(null);

        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 유저입니다");
        }

        UserType targetType = userTypeRepository.findByName(userEditInfoDto.getUserType());

        if (targetType == null) {
            throw new IllegalArgumentException("존재하지 않는 타입입니다");
        }

        user.setNickName(userEditInfoDto.getNickname());
        user.setUserType(targetType);
    }

    public UserInformationDto getUserInformation(String authorization, Long userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 유저입니다");
        }

        return dtoConverterService.convertToUserInformationDto(user);
    }

    // 이름으로 유저들을 검색하는 메서드
    @Transactional(readOnly = true)
    public Page<RequesterDto> searchUsers(String authorization, String keyword, Pageable pageable) {
        Long currentUserId = jwtService.UserIdFromToken(authorization);

        Page<User> userPage = userRepository.findByNickNameContaining(keyword, pageable);

        return userPage.map(user -> {
            if (user.getId().equals(currentUserId)) {
                return null; // null 을 반환하면 최종 결과에서 자동으로 제외됩니다.
            }

            // 캐릭터 정보를 조회합니다.
            String characterImageUrl = userCharacterRepository.findByUser_IdAndIsEquippedTrue(user.getId())
                    .map(userCharacter -> userCharacter.getCharacterImage().getImage()) // 값이 있으면 이미지 URL을 꺼내고
                    .orElse(null);

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
