package com.jason.goalwithproject.service;

import com.jason.goalwithproject.domain.user.User;
import com.jason.goalwithproject.domain.user.UserRepository;
import com.jason.goalwithproject.domain.user.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StopWatch;

import java.util.Optional;

import static org.mockito.Mockito.when;

@SpringBootTest
public class UserServicePerformanceTest {
    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("캐싱 성능 테스트: 첫 조회(DB) vs 두 번째 조회(Redis) 속도 비교")
    void cacheSpeedTest() throws InterruptedException {
        // DB에서 조회할 때 0.5초(500ms)가 걸리는 아주 무거운 쿼리라고 가정합니다.
        Long userId = 1L;
        UserType mockUserType = UserType.builder().id(1).name("새내기").build();
        User mockUser = User.builder()
                .id(userId)
                .email("test@test.com")
                .userType(mockUserType)
                .build();

        when(userRepository.findById(userId)).thenAnswer(invocation -> {
            Thread.sleep(500); // 0.5초 지연 (DB 병목 시뮬레이션)
            return Optional.of(mockUser);
        });

        // 1. 첫 번째 조회 (캐시 Miss -> DB 조회)
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        userService.getUserInfo(userId);

        stopWatch.stop();
        long noCacheTime = stopWatch.getTotalTimeMillis();
        System.out.println("캐시 없을 때 (DB 조회): " + noCacheTime + " ms");


        // 2. 두 번째 조회 (캐시 Hit -> Redis 조회)
        stopWatch = new StopWatch();
        stopWatch.start();

        userService.getUserInfo(userId); // 두 번째는 DB 안 가고 캐시에서 바로 나옴

        stopWatch.stop();
        long withCacheTime = stopWatch.getTotalTimeMillis();
        System.out.println("캐시 적용 후 (Redis 조회): " + withCacheTime + " ms");

        org.assertj.core.api.Assertions.assertThat(withCacheTime).isLessThan(noCacheTime);

        // 결과 출력
        System.out.println("최종 성능 향상: 약 " + (noCacheTime / (withCacheTime == 0 ? 1 : withCacheTime)) + "배 빨라짐");
    }
}
