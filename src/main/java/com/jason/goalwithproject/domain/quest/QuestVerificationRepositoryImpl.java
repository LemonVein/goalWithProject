package com.jason.goalwithproject.domain.quest;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.jason.goalwithproject.domain.quest.QQuest.quest;
import static com.jason.goalwithproject.domain.quest.QQuestRecord.questRecord;
import static com.jason.goalwithproject.domain.quest.QQuestVerification.questVerification;

@RequiredArgsConstructor
public class QuestVerificationRepositoryImpl implements QuestVerificationRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Quest> findDistinctQuestsCommentedByUser(Long userId, Pageable pageable) {

        // 데이터 조회
        List<Quest> content = queryFactory
                .select(questVerification.quest) // qv.quest 선택
                .distinct()
                .from(questVerification)
                .join(questVerification.quest, quest)
                .where(questVerification.user.id.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 카운트 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(questVerification.quest.countDistinct())
                .from(questVerification)
                .where(questVerification.user.id.eq(userId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public long countVerificationsOnOthersContent(Long userId) {
        return queryFactory
                .select(questVerification.count())
                .from(questVerification)
                .leftJoin(questVerification.questRecord, questRecord)
                .leftJoin(questVerification.quest, quest)
                .where(
                        questVerification.user.id.eq(userId), // 작성자는 나

                        // (직접 인증 && 퀘스트 주인 != 나) OR (기록 댓글 && 기록 주인 != 나)
                        isDirectVerificationOnOthers(userId)
                                .or(isRecordCommentOnOthers(userId))
                )
                .fetchOne(); // count()는 결과가 하나
    }

    @Override
    public long countVerificationsReceivedByUser(Long userId) {
        return queryFactory
                .select(questVerification.count())
                .from(questVerification)
                .leftJoin(questVerification.questRecord, questRecord)
                .leftJoin(questVerification.quest, quest)
                .where(
                        questVerification.user.id.ne(userId), // 작성자는 내가 아님 (남)

                        // (직접 인증 && 퀘스트 주인 == 나) OR (기록 댓글 && 기록 주인 == 나)
                        isDirectVerificationToMe(userId)
                                .or(isRecordCommentToMe(userId))
                )
                .fetchOne();
    }

    @Override
    public long countQuestVerificationsOnOthers(Long userId) {
        return queryFactory
                .select(questVerification.count())
                .from(questVerification)
                .leftJoin(questVerification.quest, quest)
                .where(
                        questVerification.user.id.eq(userId),     // 작성자는 나
                        questVerification.questRecord.isNull(),   // 레코드 댓글 아님 (직접 인증)
                        quest.user.id.ne(userId)                  // 내 퀘스트 아님
                )
                .fetchOne();
    }

    // 조건: 퀘스트 직접 인증이면서 + 퀘스트 주인이 targetId가 아님 (남의 퀘스트)
    private BooleanExpression isDirectVerificationOnOthers(Long myId) {
        return questVerification.questRecord.isNull()
                .and(questVerification.quest.user.id.ne(myId));
    }

    // 조건: 퀘스트 기록 댓글이면서 + 기록 주인이 targetId가 아님 (남의 기록)
    private BooleanExpression isRecordCommentOnOthers(Long myId) {
        return questVerification.questRecord.isNotNull()
                .and(questVerification.questRecord.user.id.ne(myId));
    }

    // 조건: 퀘스트 직접 인증이면서 + 퀘스트 주인이 targetId임 (내 퀘스트)
    private BooleanExpression isDirectVerificationToMe(Long myId) {
        return questVerification.questRecord.isNull()
                .and(questVerification.quest.user.id.eq(myId));
    }

    // 조건: 퀘스트 기록 댓글이면서 + 기록 주인이 targetId임 (내 기록)
    private BooleanExpression isRecordCommentToMe(Long myId) {
        return questVerification.questRecord.isNotNull()
                .and(questVerification.questRecord.user.id.eq(myId));
    }

}
