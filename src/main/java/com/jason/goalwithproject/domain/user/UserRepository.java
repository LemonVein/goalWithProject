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
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    boolean existsByEmail(String email);
    boolean existsByNickName(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByUserType(UserType userType);

    @Query("SELECT u FROM User u WHERE u.nickName LIKE %:keyword% AND u.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE " +
            "AND u.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN")
    Page<User> searchActiveUsers(@Param("keyword") String keyword, Pageable pageable);

    Page<User> findByNickNameContaining(String name, Pageable pageable);

    Page<User> findByLevelBetweenAndIdNot(int minLevel, int maxLevel, Long userId, Pageable pageable);

    @Query("SELECT u FROM User u " +
            "WHERE u.level BETWEEN :minLevel AND :maxLevel " +
            "AND u.id <> :userId " +
            "AND u.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE " +
            "AND u.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN") // 🔥 여기 수정됨
    Page<User> findRecommendCandidates(@Param("minLevel") int minLevel,
                                       @Param("maxLevel") int maxLevel,
                                       @Param("userId") Long userId,
                                       Pageable pageable);

    // Provider 로 찾기
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
