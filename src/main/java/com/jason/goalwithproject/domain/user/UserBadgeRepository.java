package com.jason.goalwithproject.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Integer> {
    UserBadge findByUser_Id(Long id);
}
