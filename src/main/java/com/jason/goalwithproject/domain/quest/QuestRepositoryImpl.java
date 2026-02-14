package com.jason.goalwithproject.domain.quest;

import com.jason.goalwithproject.domain.user.Role;
import com.jason.goalwithproject.domain.user.UserStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import static com.jason.goalwithproject.domain.quest.QQuest.quest;

import java.util.List;

@RequiredArgsConstructor
public class QuestRepositoryImpl implements QuestRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    private BooleanExpression isQuestOwnerActiveNormalUser() {
        return quest.user.userStatus.eq(UserStatus.ACTIVE)
                .and(quest.user.role.ne(Role.ROLE_ADMIN));
    }

    // 키워드 검색
    @Override
    public List<Quest> findVerifiableQuestsByKeyword(QuestStatus status, String keyword) {
        return queryFactory
                .selectFrom(quest)
                .where(
                        quest.questStatus.eq(status),
                        quest.verificationRequired.isTrue(),
                        isQuestOwnerActiveNormalUser(),

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
                        quest.verificationRequired.isTrue(),
                        isQuestOwnerActiveNormalUser()
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

    @Override
    public Page<Quest> findPeerQuestsForVerification(List<Long> peerIds, QuestStatus status, String keyword, Pageable pageable) {
        QQuest quest = QQuest.quest;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(quest.user.id.in(peerIds)); // 친구들이 작성한 퀘스트
        builder.and(quest.questStatus.eq(status)); // 상태 (VERIFY)
        builder.and(isQuestOwnerActiveNormalUser());

        if (keyword != null && !keyword.isBlank()) {
            builder.and(
                    quest.title.containsIgnoreCase(keyword)        // 제목 검색
                            .or(quest.description.containsIgnoreCase(keyword)) // 설명 검색
            );
        }

        List<Quest> content = queryFactory
                .selectFrom(quest)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(quest.startDate.desc()) // 최신순 정렬
                .fetch();

        Long total = queryFactory
                .select(quest.count())
                .from(quest)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public List<Quest> findCandidatesForRecommendation(QuestStatus status, String keyword) {
        QQuest quest = QQuest.quest;

        BooleanBuilder builder = new BooleanBuilder();

        builder.and(quest.questStatus.eq(status));
        builder.and(quest.verificationRequired.isTrue());
        builder.and(isQuestOwnerActiveNormalUser());

        // 키워드가 있을 때만 적용 (제목 or 설명)
        if (keyword != null && !keyword.isBlank()) {
            builder.and(
                    quest.title.containsIgnoreCase(keyword)
                            .or(quest.description.containsIgnoreCase(keyword))
            );
        }

        return queryFactory
                .selectFrom(quest)
                .where(builder)
                .fetch();
    }

}
