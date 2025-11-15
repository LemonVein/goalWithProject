package com.jason.goalwithproject.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Integer> {
    UserBadge findByUser_Id(Long id);

    UserBadge findByUser_IdAndEquippedTrue(Long id);

    List<UserBadge> findAllByUser_Id(Long id);

    // 해당 뱃지를 가지고 있는 확인 (업적용)
    boolean existsByUser_IdAndBadge_Id(Long userId, int badgeId);
}
