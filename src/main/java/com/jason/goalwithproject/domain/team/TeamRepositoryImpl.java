package com.jason.goalwithproject.domain.team;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.jason.goalwithproject.domain.team.QTeam.team;
import static com.jason.goalwithproject.domain.quest.QQuest.quest;

@RequiredArgsConstructor
public class TeamRepositoryImpl implements TeamRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Team> findByNameOrQuestTitle(String keyword, Pageable pageable) {

        List<Team> content = queryFactory
                .selectFrom(team)
                .distinct()
                .leftJoin(quest).on(quest.team.eq(team)) // LEFT JOIN Quest q ON q.team = t
                .where(
                        containKeyword(keyword)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(team.countDistinct())
                .from(team)
                .leftJoin(quest).on(quest.team.eq(team))
                .where(
                        containKeyword(keyword)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // 검색 조건 생성 메서드
    private BooleanExpression containKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        // 팀 이름에 포함되거나 퀘스트 제목에 포함
        return team.name.contains(keyword)
                .or(quest.title.contains(keyword));
    }
}
