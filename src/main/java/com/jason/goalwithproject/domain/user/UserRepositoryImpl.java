package com.jason.goalwithproject.domain.user;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static com.jason.goalwithproject.domain.user.QUser.user;
import static com.jason.goalwithproject.domain.user.QUserCharacter.userCharacter;
import static com.jason.goalwithproject.domain.quest.QQuestRecord.questRecord;

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
}
