package com.jason.goalwithproject.domain.quest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestVerificationRepositoryCustom {
    // 특정 사용자가 타인의 퀘스트에 인증을 날린 퀘스트 목록
    Page<Quest> findDistinctQuestsCommentedByUser(Long userId, Pageable pageable);
    // 특정 사용자가 받은 인증 및 댓글 수
    long countVerificationsReceivedByUser(Long userId);
    // 특정 사용자가 타인의 컨텐츠에 남긴 인증 수
    long countVerificationsOnOthersContent(Long userId);
    // 사용자가 직접 인증 카운트
    long countQuestVerificationsOnOthers(Long userId);
}
