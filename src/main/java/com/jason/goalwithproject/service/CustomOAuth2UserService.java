package com.jason.goalwithproject.service;

import com.jason.goalwithproject.domain.custom.Badge;
import com.jason.goalwithproject.domain.custom.BadgeRepository;
import com.jason.goalwithproject.domain.custom.CharacterImage;
import com.jason.goalwithproject.domain.custom.CharacterImageRepository;
import com.jason.goalwithproject.domain.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final CharacterImageRepository characterImageRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final BadgeRepository badgeRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(); // "sub"

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name"); // 구글에서 제공하는 이름
        // String picture = (String) attributes.get("picture"); // 프로필 사진 URL

        // DB에서 이메일로 사용자 조회
        Optional<User> userOptional = userRepository.findByEmail(email);

        User user;
        if (userOptional.isPresent()) {
            // 이미 가입된 사용자
            user = userOptional.get();
        } else {
            // 처음 방문한 사용자 -> 자동 회원가입
            user = new User();
            user.setEmail(email);
            user.setName(name); // 실제 이름
            user.setNickName(generateUniqueNickname(name)); // 닉네임 자동 생성 (중복 처리 필요)
            user.setPassword(""); // 소셜 로그인이므로 비밀번호는 비워둠
            UserType defaultUserType = userTypeRepository.findById(1).orElse(null); // ID 1번이 기본 타입
            user.setUserType(defaultUserType);
            user = userRepository.save(user);

            // 회원가입 시 기본 캐릭터 및 뱃지 설정 (기존 UserService 로직 참고)
            setDefaultCharacterAndBadge(user);
        }

        // Spring Security Context에 저장될 사용자 정보 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), // 기본 권한
                attributes,
                userNameAttributeName);
    }

    private String generateUniqueNickname(String baseName) {
        String nickname = baseName.replaceAll("\\s+", ""); // 공백 제거
        if (userRepository.existsByNickName(nickname)) {
            // 중복 시 플랜은 일단 보류
            return nickname + System.currentTimeMillis() % 1000;
        }
        return nickname;
    }

    private void setDefaultCharacterAndBadge(User user) {
        CharacterImage characterImage = characterImageRepository.findById(1);
        UserCharacter userCharacter = new UserCharacter();
        userCharacter.setUser(user);
        userCharacter.setCharacterImage(characterImage);
        userCharacter.setEquipped(true);
        userCharacterRepository.save(userCharacter);

        Badge badge = badgeRepository.findById(1).orElse(null);

        userBadgeRepository.findById(1).ifPresent(userBadge -> {
            userBadge.setUser(user);
            userBadge.setBadge(badge);
            userBadgeRepository.save(userBadge);
        });
    }
}
