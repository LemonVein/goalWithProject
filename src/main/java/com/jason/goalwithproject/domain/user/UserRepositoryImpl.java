package com.jason.goalwithproject.domain.user;

import com.jason.goalwithproject.domain.quest.Quest;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static com.jason.goalwithproject.domain.user.QUser.user;
import static com.jason.goalwithproject.domain.user.QUserCharacter.userCharacter;
import static com.jason.goalwithproject.domain.quest.QQuestRecord.questRecord;
import static com.jason.goalwithproject.domain.quest.QQuest.quest;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<User> findUsersEligibleForLazyCharacter(LocalDateTime cutoffDate, int characterId) {

        return queryFactory
                .selectFrom(user)
                .where(
                        // 가입한 지 7일 지났고 (createdAt <= cutoffDate)
                        user.createdAt.loe(cutoffDate),

                        // 최근 7일간 기록이 없는 사람
                        user.id.notIn(
                                JPAExpressions
                                        .select(questRecord.user.id)
                                        .from(questRecord)
                                        .where(questRecord.createdAt.goe(cutoffDate)) // >= cutoffDate
                        ),

                        // 이미 해당 캐릭터를 받은 사람은 제외
                        user.id.notIn(
                                JPAExpressions
                                        .select(userCharacter.user.id)
                                        .from(userCharacter)
                                        .where(userCharacter.characterImage.id.eq(characterId))
                        )
                )
                .fetch();
    }

    @Override
    public Page<User> findArbitraryUsers(Pageable pageable) {

        List<User> content = queryFactory
                .selectFrom(user)
                // 🔥 핵심: 이메일이 "admin_created_"로 시작하는 데이터만 필터링!
                .where(user.email.startsWith("admin_created_"))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(user.id.desc()) // 최신순 정렬
                .fetch();

        // 전체 개수 카운트 쿼리
        long total = queryFactory
                .select(user.count())
                .from(user)
                .where(user.email.startsWith("admin_created_"))
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }
}
