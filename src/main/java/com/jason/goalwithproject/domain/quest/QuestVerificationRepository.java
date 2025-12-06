package com.jason.goalwithproject.domain.quest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestVerificationRepository extends JpaRepository<QuestVerification, Long> {
    List<QuestVerification> findAllByQuest_IdAndUser_Id(Long questId, Long userId);
    List<QuestVerification> findAllByQuestRecord_Id(Long questRecordId);
    List<QuestVerification> findAllByQuest_Id(Long questId);
    Optional<QuestVerification> findById(Long id);

    int countByQuest_Id(Long questId);

    void deleteByQuestRecord_Id(Long id);
    void deleteAllByQuestRecord_Id(Long id);

    boolean existsByUser_IdAndQuest_Id(Long userId, Long questId);

    @Query("SELECT DISTINCT qv.quest FROM QuestVerification qv WHERE qv.user.id = :userId")
    Page<Quest> findDistinctQuestsCommentedByUser(@Param("userId") Long userId, Pageable pageable);

    // 특정 사용자가 타인의 콘텐츠에 남긴 인증 수 카운트
    @Query("SELECT COUNT(qv) FROM QuestVerification qv " +
            "WHERE qv.user.id = :userId " +
            "AND (" +
            "  (qv.questRecord IS NULL AND qv.quest.user.id != :userId) " + // 퀘스트 직접 인증
            "  OR " +
            "  (qv.questRecord IS NOT NULL AND qv.questRecord.user.id != :userId) " + // 퀘스트 기록에 댓글
            ")")
    long countVerificationsOnOthersContent(@Param("userId") Long userId);

    // 특정 사용자가 받은 인증/댓글 수 조회
    @Query("SELECT COUNT(qv) FROM QuestVerification qv " +
            "LEFT JOIN qv.questRecord qr " +
            "WHERE qv.user.id != :userId " +
            "AND (" +
            "  (qr IS NULL AND qv.quest.user.id = :userId) " + // qv.questRecord 대신 별칭 qr 사용
            "  OR " +
            "  (qr IS NOT NULL AND qr.user.id = :userId)" + // qv.questRecord 대신 별칭 qr 사용
            ")")
    long countVerificationsReceivedByUser(@Param("userId") Long userId);

    // 사용자가 다른 사용자에게 인증한 카운트
    @Query("SELECT COUNT(qv) FROM QuestVerification qv " +
            "WHERE qv.user.id = :userId " +
            "AND qv.questRecord IS NULL " + // 레코드 댓글이 아닌 퀘스트 인증만
            "AND qv.quest.user.id != :userId") // 내 퀘스트가 아닌 타인의 퀘스트
    long countQuestVerificationsOnOthers(@Param("userId") Long userId);
}
