package com.jason.goalwithproject.domain.quest;

import com.jason.goalwithproject.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestVerificationReportRepository extends JpaRepository<QuestVerificationReport, Long> {
    boolean existsByReporter_IdAndVerification_Id(Long reporter, Long verification);
}
