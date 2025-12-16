package com.jason.goalwithproject.domain.user;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepositoryCustom {
    List<User> findUsersEligibleForLazyCharacter(LocalDateTime cutoffDate, int characterId);
}
