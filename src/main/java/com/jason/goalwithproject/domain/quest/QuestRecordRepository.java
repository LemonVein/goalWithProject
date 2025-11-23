package com.jason.goalwithproject.domain.quest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface QuestRecordRepository extends JpaRepository<QuestRecord, Long> {
    Optional<QuestRecord> findByQuest_Id(Long id);
    Page<QuestRecord> findAllByQuest_Id(Long id, Pageable pageable);
    List<QuestRecord> findAllByQuest_Id(Long id);
    int countByQuest_Id(Long questId);

    /**
     * 특정 사용자가 특정 날짜 이후에 기록을 작성한 모든 '날짜'를 중복 없이 조회합니다.
     * (createdAt은 TIMESTAMP이므로, DATE() 함수를 사용해 날짜 부분만 추출합니다)
     */
    // FUNCTION('DATE', ...) 대신 CAST(... AS LocalDate)를 사용합니다.
    @Query("SELECT DISTINCT CAST(qr.createdAt AS LocalDate) FROM QuestRecord qr WHERE qr.user.id = :userId AND qr.createdAt >= :startDate")
    Set<LocalDate> findDistinctRecordDatesByUserSince(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
}
