package com.jason.goalwithproject.domain.user;

import com.jason.goalwithproject.domain.quest.Quest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepositoryCustom {
    List<User> findUsersEligibleForLazyCharacter(LocalDateTime cutoffDate, int characterId);
    Page<User> findArbitraryUsers(Pageable pageable);
}
