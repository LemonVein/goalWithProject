package com.jason.goalwithproject.service;

import com.jason.goalwithproject.domain.custom.CharacterImage;
import com.jason.goalwithproject.domain.custom.CharacterImageRepository;
import com.jason.goalwithproject.domain.user.User;
import com.jason.goalwithproject.domain.user.UserCharacter;
import com.jason.goalwithproject.domain.user.UserCharacterRepository;
import com.jason.goalwithproject.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AchievementScheduler {

    private final UserRepository userRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final CharacterImageRepository characterImageRepository;

    // 쉬는 피코 이미지 데이터베이스 아이디
    private static final int REST_PICO_ID = 2;

    // 매일 자정(00:00:00)에 실행
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void giveRestPicoTask() {

        log.info("SCHEDULER START: 쉬는 피코 캐릭터 지급 작업 시작");

        // 기준 시간 설정 (7일 전)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 대상 사용자 조회 (쿼리 한 번으로 최적화)
        List<User> lazyUsers = userRepository.findUsersEligibleForLazyCharacter(sevenDaysAgo, REST_PICO_ID);

        if (lazyUsers.isEmpty()) {
            log.info("SCHEDULER END: 지급 대상자가 없습니다.");
            return;
        }

        // 캐릭터 정보 조회
        CharacterImage lazyCharacterImage = characterImageRepository.findById(REST_PICO_ID);
        if (lazyCharacterImage == null) {
            log.error("SCHEDULER ERROR: ID {}번 캐릭터 이미지를 찾을 수 없습니다.", REST_PICO_ID);
            return;
        }

        // 대상자들에게 캐릭터 일괄 지급
        for (User user : lazyUsers) {
            UserCharacter userCharacter = new UserCharacter();
            userCharacter.setUser(user);
            userCharacter.setCharacterImage(lazyCharacterImage);
            userCharacter.setEquipped(false);

            userCharacterRepository.save(userCharacter);

            log.info("GIVE CHARACTER: User {} 님에게 '{}' 캐릭터 지급 완료", user.getId(), lazyCharacterImage.getName());
        }

        log.info("SCHEDULER END: 총 {}명에게 쉬는 피코 캐릭터 지급 완료", lazyUsers.size());
    }
}
