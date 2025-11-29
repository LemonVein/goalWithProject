package com.jason.goalwithproject.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickName(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByUserType(UserType userType);

    Page<User> findByNickNameContaining(String name, Pageable pageable);

    Page<User> findByLevelBetweenAndIdNot(int minLevel, int maxLevel, Long userId, Pageable pageable);

    // 제미나이에게 짜달라고 한 쿼리
    @Query("SELECT u FROM User u " +
            "WHERE u.createdAt <= :cutoffDate " + // 가입한 지 7일 지났고
            "AND u.id NOT IN (" +
            "    SELECT qr.user.id FROM QuestRecord qr WHERE qr.createdAt >= :cutoffDate" + // 최근 7일간 기록이 없는 사람
            ") " +
            "AND u.id NOT IN (" +
            "    SELECT uc.user.id FROM UserCharacter uc WHERE uc.characterImage.id = :characterId" + // 이미 해당 캐릭터를 받은 사람은 제외
            ")")
    List<User> findUsersEligibleForLazyCharacter(
            @Param("cutoffDate") LocalDateTime cutoffDate,
            @Param("characterId") int characterId);
}
