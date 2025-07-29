package com.jason.goalwithproject.domain.quest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestVerificationRepository extends JpaRepository<QuestVerification, Long> {
    List<QuestVerification> findAllByQuest_IdAndUser_Id(Long questId, Long userId);
}
