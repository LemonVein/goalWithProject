package com.jason.goalwithproject.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.jason.goalwithproject.config.JwtTokenProvider;
import com.jason.goalwithproject.domain.custom.Badge;
import com.jason.goalwithproject.domain.custom.BadgeRepository;
import com.jason.goalwithproject.domain.custom.CharacterImage;
import com.jason.goalwithproject.domain.custom.CharacterImageRepository;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.common.ReportRequestDto;
import com.jason.goalwithproject.dto.custom.BadgeDto;
import com.jason.goalwithproject.dto.custom.CharacterDto;
import com.jason.goalwithproject.dto.custom.CharacterIdDto;
import com.jason.goalwithproject.dto.jwt.*;
import com.jason.goalwithproject.dto.peer.RequesterDto;
import com.jason.goalwithproject.dto.user.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
    private final RestTemplate restTemplate;
    private final AppleAuthService appleAuthService;
    private final UserReportRepository userReportRepository;
    private final CacheManager cacheManager;

    @Value("${google.api.client-id.android}")
    private String googleClientIdAndroid;

//    @Value("{google.api.client-id.android-debug")
//    private String googleClientIdAndroidDebug;

    @Value("${google.api.client-id.ios}")
    private String googleClientIdIos;

//    @Value("${google.api.client-id.ios-debug}")
//    private String googleClientIdIosDebug;

    @Value("${google.api.client-id.web}")
    private String googleClientIdWeb;

//    @Value("${google.api.client-id.web-debug}")
//    private String googleClientIdWebDebug;

    @Transactional
    public TokenResponse TryLogin(UserLoginDto userLoginDto) throws Exception {
        User user = userRepository.findByEmail(userLoginDto.getEmail()).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("이메일 또는 비밀번호가 잘못되었습니다");
        }

        if (!passwordEncoder.matches(userLoginDto.getPassword(), user.getPassword())) {
            throw new EntityNotFoundException("이메일 또는 비밀번호가 잘못되었습니다");
        }

        if (user.getUserStatus() == UserStatus.SUSPENDED) {
            throw new Exception("관리자에 의해 정지된 계정입니다.");
        }

        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "role", user.getRole()
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
                userType,
                Role.ROLE_USER,
                UserStatus.ACTIVE
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
        userBadge.setEquipped(true);
        userBadgeRepository.save(userBadge);

        Map<String, Object> claims = Map.of(
                "userId", saveUser.getId(),
                "role", user.getRole()
        );
        String accessToken = jwtTokenProvider.generateAccessToken(claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(claims);

        return new TokenResponseWithStatus(accessToken, refreshToken, "success");
    }

    @Transactional
    // userId를 키값으로 저장
    @Cacheable(value = "userInfo", key = "#userId", unless = "#result == null")
    public UserDto getUserInfo(Long userId) {

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new EntityNotFoundException("해당하는 유저가 없습니다.");
        }

        return dtoConverterService.convertToDto(user);

    }

    @Transactional
    public TokenResponse reissueToken(String refreshToken) {
        Optional<UserRefreshToken> userRefreshToken = userRefreshTokenRepository.findByToken(refreshToken);
        if (userRefreshToken.isEmpty()) {
            throw new IllegalArgumentException("리프레쉬 토큰이 만료되었습니다");
        }

        UserRefreshToken userRefreshTokenEntity = userRefreshToken.get();
        User targetUser = userRefreshTokenEntity.getUser();

        if (targetUser.getUserStatus() == UserStatus.SUSPENDED) {
            userRefreshTokenRepository.delete(userRefreshToken.get()); // 혹시 남아있다면 삭제
            throw new RuntimeException("정지된 계정입니다.");
        }

        Map<String, Object> claims = Map.of(
                "userId", targetUser.getId(),
                "role", targetUser.getRole()
        );
        String newAccessToken = jwtTokenProvider.generateAccessToken(claims);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(claims);
        LocalDateTime newExpiryTime = LocalDateTime.now().plusSeconds(jwtTokenProvider.getREFRESH_EXPIRATION_TIME() / 1000);

        userRefreshTokenEntity.updateToken(newRefreshToken, newExpiryTime);

        return new TokenResponse(newAccessToken, newRefreshToken);

    }

    @Transactional
    public List<BadgeDto> getBadges(String authorization) {
        Long userId = jwtService.UserIdFromToken(authorization);

        List<UserBadge> userBadges = userBadgeRepository.findAllByUser_Id(userId);

        List<BadgeDto> dtos = userBadges.stream().map(BadgeDto::new).toList();

        return dtos;
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
    @CacheEvict(value = "userInfo", key = "#userId")
    public void updateCharacter(String authorization, Long userId, CharacterIdDto characterIdDto) throws AccessDeniedException {
        Long myId = jwtService.UserIdFromToken(authorization);
        if (!Objects.equals(myId, userId)) {
            throw new AccessDeniedException("캐릭터를 변경할 권한이 없습니다.");
        }

        int userCharacterIdToEquip = characterIdDto.getId();

        boolean equippedCharacter = userCharacterRepository.existsByUser_IdAndCharacterImage_Id(myId, userCharacterIdToEquip);
        if (!equippedCharacter) {throw new EntityNotFoundException("해당 캐릭터를 소유하고 있지 않습니다.");}


        userCharacterRepository.findByUser_IdAndIsEquippedTrue(userId).ifPresent(oldEquippedCharacter -> {
            oldEquippedCharacter.setEquipped(false);
        });

        UserCharacter newCharacter =  userCharacterRepository.findByUser_IdAndCharacterImage_Id(myId, characterIdDto.getId());
        newCharacter.setEquipped(true);
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

        // 추후에 프론트에서 그냥 유저 아이디를 받게되면 수정할 예정
        // 직접 캐시 삭제하기
        cacheManager.getCache("userInfo").evict(currentUserId);
    }

    @Transactional(readOnly = true)
    public UserInformationDto getUserInformation(String authorization, Long userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 유저입니다");
        }

        if (user.getUserStatus() == UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("정지된 유저입니다.");
        }

        if (user.getUserStatus() == UserStatus.WITHDRAWN) {
            throw new IllegalArgumentException("삭제된 유저입니다.");
        }

        return dtoConverterService.convertToUserInformationDto(user);
    }

    @Transactional
    public void updateBadge(String authorization, int badgeId) {
        Long userId = jwtService.UserIdFromToken(authorization);

        boolean isHasBadge = userBadgeRepository.existsByUser_IdAndBadge_Id(userId, badgeId);
        if (isHasBadge) {
            UserBadge userBadge = userBadgeRepository.findByUser_IdAndEquippedTrue(userId).get();
            userBadge.setEquipped(false);

            UserBadge changedBadge = userBadgeRepository.findByUser_IdAndBadge_Id(userId, badgeId);
            changedBadge.setEquipped(true);

            userBadgeRepository.save(userBadge);
            userBadgeRepository.save(changedBadge);

            // 캐시 직접 삭제 메서드
            cacheManager.getCache("userInfo").evict(userId);
        } else {
            throw new EntityNotFoundException("해당 뱃지를 얻지 않았습니다.");
        }
    }

    // 이름으로 유저들을 검색하는 메서드
    @Transactional(readOnly = true)
    public Page<RequesterDto> searchUsers(String authorization, String keyword, Pageable pageable) {
        Long currentUserId = jwtService.UserIdFromToken(authorization);

        Page<User> userPage = userRepository.searchActiveUsers(keyword, pageable);

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

    @Transactional
    public GoogleAuthTokenResponse authenticateApple(GoogleTokenDto request) {
        // 애플 서버와 통신하여 유효성 검증 및 고유 ID(sub) 획득
        AppleInfo appleInfo = appleAuthService.getAppleUserInfo(request.getToken());
        String appleUniqueId = appleInfo.getSub();
        String email = appleInfo.getEmail();

        // DB에서 해당 애플 ID를 가진 유저가 있는지 확인
        // (User 엔티티에 provider="APPLE", providerId=appleUniqueId 로 저장한다고 가정)
        Optional<User> optionalUser = userRepository.findByProviderAndProviderId("APPLE", appleUniqueId);

        AtomicReference<Boolean> isNewer = new AtomicReference<>(false);

        User user;
        if (optionalUser.isEmpty()) {
            // 없으면 회원가입 (신규 유저)
            user = new User();
            if (email != null && !email.isBlank()) {
                user.setEmail(email);
            } else {
                // 이메일 가리기 등을 사용해 이메일이 없는 경우 임시 이메일 생성
                user.setEmail(appleUniqueId.substring(0, 10) + "@apple.login");
            }
            String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
            user.setName("Guest_" + randomSuffix);
            user.setNickName("Guest_" + randomSuffix);
            user.setProvider("APPLE");
            user.setPassword(""); // 소셜로그인
            user.setProviderId(appleUniqueId);
            user.setRole(Role.ROLE_USER);
            user.setUserStatus(UserStatus.ACTIVE);
            isNewer.set(true);
            UserType defaultUserType = userTypeRepository.findById(1).orElse(null);
            user.setUserType(defaultUserType);

            userRepository.save(user);

            setDefaultCharacterAndBadge(user);
        } else {
            // 있으면 로그인 처리
            user = optionalUser.get();
        }

        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "role", user.getRole()
        );
        String accessToken = jwtTokenProvider.generateAccessToken(claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(claims);

        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(jwtTokenProvider.getREFRESH_EXPIRATION_TIME() / 1000);
        userRefreshTokenRepository.findByUser_Id(user.getId())
                .ifPresentOrElse(urt -> urt.updateToken(refreshToken, expiryTime),
                        () -> userRefreshTokenRepository.save(new UserRefreshToken(user, refreshToken, expiryTime)));

        return GoogleAuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewer(isNewer.get())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    // 구글 로그인 처리 메서드
    @Transactional
    public GoogleAuthTokenResponse authenticateGoogle(GoogleTokenDto googleIdTokenString) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                // 허용할 클라이언트 ID 목록을 리스트로 전달합니다.
                .setAudience(Arrays.asList(googleClientIdAndroid, googleClientIdIos, googleClientIdWeb))
                // 디버그 환경에서 사용할 키들
//                        googleClientIdAndroidDebug, googleClientIdWebDebug, googleClientIdIosDebug))
                .build();

        // 토큰 검증 및 파싱 (실패 시 null 반환 또는 예외 발생)
        GoogleIdToken idToken = verifier.verify(googleIdTokenString.getToken());
        if (idToken == null) {
            throw new IllegalArgumentException("유효하지 않은 구글 ID 토큰입니다.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        AtomicReference<Boolean> isNewer = new AtomicReference<>(false);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 처음 방문한 사용자 -> 자동 회원가입
                    User newUser = new User();
                    newUser.setProvider("GOOGLE");
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setNickName(generateUniqueNickname(name)); // 닉네임 자동 생성
                    newUser.setPassword(""); // 소셜 로그인
                    UserType defaultUserType = userTypeRepository.findById(1).orElse(null);
                    newUser.setUserType(defaultUserType);
                    newUser.setRole(Role.ROLE_USER);
                    newUser.setUserStatus(UserStatus.ACTIVE);

                    // 저장
                    newUser = userRepository.save(newUser);
                    setDefaultCharacterAndBadge(newUser); // 기본 캐릭터/뱃지 설정
                    isNewer.set(true);
                    return newUser;
                });

        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "role", user.getRole()
        );
        String accessToken = jwtTokenProvider.generateAccessToken(claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(claims);

        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(jwtTokenProvider.getREFRESH_EXPIRATION_TIME() / 1000);
        userRefreshTokenRepository.findByUser_Id(user.getId())
                .ifPresentOrElse(urt -> urt.updateToken(refreshToken, expiryTime),
                        () -> userRefreshTokenRepository.save(new UserRefreshToken(user, refreshToken, expiryTime)));

        return GoogleAuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewer(isNewer.get())
                .email(email)
                .name(name)
                .build();

    }

    @Transactional
    public GoogleAuthTokenResponse authenticateKakao(KakaoTokenDto kakaoTokenDto) throws GeneralSecurityException, IOException {
        Map<String, Object> kakaoUserInfo = getKakaoUserInfo(kakaoTokenDto.getAccessToken());

        Map<String, Object> kakaoAccount = (Map<String, Object>) kakaoUserInfo.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String email = (String) kakaoAccount.get("email");
        String name = (String) profile.get("nickname");

        if (email == null) {
            // 에러를 발생시켜 프론트에서 이메일 동의를 다시 받도록 함
            throw new IllegalArgumentException("카카오 로그인 시 이메일 제공에 동의해야 합니다.");
        }

        AtomicReference<Boolean> isNewer = new AtomicReference<>(false);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 처음 방문한 사용자 -> 자동 회원가입
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setNickName(generateUniqueNickname(name)); // 닉네임 자동 생성
                    newUser.setPassword(""); // 소셜 로그인
                    UserType defaultUserType = userTypeRepository.findById(1).orElse(null);
                    newUser.setUserType(defaultUserType);
                    newUser.setRole(Role.ROLE_USER);

                    // 저장
                    newUser = userRepository.save(newUser);
                    setDefaultCharacterAndBadge(newUser); // 기본 캐릭터/뱃지 설정
                    isNewer.set(true);
                    return newUser;
                });

        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "role", user.getRole()
        );
        String accessToken = jwtTokenProvider.generateAccessToken(claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(claims);

        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(jwtTokenProvider.getREFRESH_EXPIRATION_TIME() / 1000);
        userRefreshTokenRepository.findByUser_Id(user.getId())
                .ifPresentOrElse(urt -> urt.updateToken(refreshToken, expiryTime),
                        () -> userRefreshTokenRepository.save(new UserRefreshToken(user, refreshToken, expiryTime)));

        return GoogleAuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewer(isNewer.get())
                .email(email)
                .name(name)
                .build();

    }

    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class  // 응답은 Map 형태로 받습니다.
            );

            // 6. 요청 성공 시, 응답 본문(사용자 정보가 담긴 Map)을 반환합니다.
            return response.getBody();

        } catch (Exception e) {
            // 만약 토큰이 만료되었거나 유효하지 않으면 요청이 실패하고 예외가 발생합니다.
            throw new IllegalArgumentException("유효하지 않은 카카오 토큰입니다.", e);
        }
    }

    @Transactional
    @CacheEvict(value = "userInfo", key = "#user.id")
    public void addExpAndProcessLevelUp(User user, int expToAdd) {
        user.setExp(user.getExp() + expToAdd);

        int requiredExp = getRequiredExpForLevel(user.getLevel());

        // 여러 번 레벨업할 수도 있으므로
        while (user.getExp() >= requiredExp) {
            user.setLevel(user.getLevel() + 1);

            user.setExp(user.getExp() - requiredExp);

            // 레벨업 보상 보류
            // user.setActionPoint(user.getActionPoint() + 10);

            // 다음 레벨의 필요 경험치를 다시 계산
            requiredExp = getRequiredExpForLevel(user.getLevel());
        }
    }

    // 유저 삭제 (추후 논리적 삭제로 변경할 예정 26. 01. 01 기준)
    @Transactional
    public void revokeUser(String authorization) {
        Long userId = jwtService.UserIdFromToken(authorization);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        user.setUserStatus(UserStatus.WITHDRAWN);

        userRefreshTokenRepository.deleteByUser_Id(userId);

        user.setName("탈퇴한 사용자");
        user.setNickName("Unknown User");
        user.setEmail("deleted_" + user.getId() + "@deleted.com");
    }

    @Transactional
    public void reportUser(String authorization, Long targetUserId, ReportRequestDto reportRequestDto) throws AccessDeniedException {
        Long reporterId = jwtService.UserIdFromToken(authorization);

        // 신고자 조회
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new EntityNotFoundException("신고자를 찾을 수 없습니다."));

        // 신고 대상(신고 당한 사람) 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("신고 대상을 찾을 수 없습니다."));

        if (reporter.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("자기 자신은 신고할 수 없습니다.");
        }

        if (userReportRepository.existsByReporter_IdAndTarget_Id(reporter.getId(), targetUser.getId())) {
            throw new IllegalStateException("이미 신고한 유저입니다.");
        }

        UserReport report = UserReport.builder()
                .reporter(reporter)
                .target(targetUser)
                .reason(reportRequestDto.getReason())
                .build();

        userReportRepository.save(report);
    }

    // 카카오나 구글 계정 이용시 닉네임 자동 생성 시 중복 방지용 메서드
    private String generateUniqueNickname(String baseName) {
        String nickname = baseName.replaceAll("\\s+", ""); // 공백 제거
        if (userRepository.existsByNickName(nickname)) {
            // 중복 시 플랜은 일단 보류
            return nickname + System.currentTimeMillis() % 1000;
        }
        return nickname;
    }

    // 디폴트 캐릭터와 뱃지를 설정하는 메서드
    private void setDefaultCharacterAndBadge(User user) {
        CharacterImage characterImage = characterImageRepository.findById(1);
        UserCharacter userCharacter = new UserCharacter();
        userCharacter.setUser(user);
        userCharacter.setCharacterImage(characterImage);
        userCharacter.setEquipped(true);
        userCharacterRepository.save(userCharacter);

        Badge badge = badgeRepository.findById(1).orElse(null);

        UserBadge userBadge = new UserBadge();
        userBadge.setUser(user);
        userBadge.setBadge(badge);
        userBadge.setEquipped(true);
        userBadgeRepository.save(userBadge);

    }

    // 다음 레벨까지 필요한 경험치량 계산 메서드
    private int getRequiredExpForLevel(int currentLevel) {
        return (int) (200 * (currentLevel * 1.1));
    }
}
