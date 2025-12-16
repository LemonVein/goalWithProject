package com.jason.goalwithproject.domain.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamRepositoryCustom {
    Page<Team> findByNameOrQuestTitle(String keyword, Pageable pageable);
}
