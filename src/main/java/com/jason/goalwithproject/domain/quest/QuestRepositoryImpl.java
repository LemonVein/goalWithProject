package com.jason.goalwithproject.domain.quest;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import static com.jason.goalwithproject.domain.quest.QQuest.quest;

import java.util.List;

@RequiredArgsConstructor
public class QuestRepositoryImpl implements QuestRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    // 키워드 검색
    @Override
    public List<Quest> findVerifiableQuestsByKeyword(QuestStatus status, String keyword) {
        return queryFactory
                .selectFrom(quest)
                .where(
                        quest.questStatus.eq(status),
                        quest.verificationRequired.isTrue(),

                        quest.title.containsIgnoreCase(keyword)
                                .or(quest.description.containsIgnoreCase(keyword))
                )
                .fetch();
    }

    // 동료 퀘스트 페이징 조회
    @Override
    public Page<Quest> findPeerQuestsForVerification(List<Long> peerIds, QuestStatus status, Pageable pageable) {

        // (1) 데이터 조회 쿼리
        List<Quest> content = queryFactory
                .selectFrom(quest)
                .where(
                        quest.user.id.in(peerIds),           // IN :peerIds
                        quest.questStatus.eq(status),
                        quest.verificationRequired.isTrue()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(quest.count())
                .from(quest)
                .where(
                        quest.user.id.in(peerIds),
                        quest.questStatus.eq(status),
                        quest.verificationRequired.isTrue()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

}
