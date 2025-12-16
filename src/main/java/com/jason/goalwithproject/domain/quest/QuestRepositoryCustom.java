package com.jason.goalwithproject.domain.quest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface QuestRepositoryCustom {
    List<Quest> findVerifiableQuestsByKeyword(QuestStatus status, String keyword);
    Page<Quest> findPeerQuestsForVerification(List<Long> peerIds, QuestStatus status, Pageable pageable);
}
