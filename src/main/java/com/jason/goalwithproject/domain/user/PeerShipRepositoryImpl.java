package com.jason.goalwithproject.domain.user;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

import static com.jason.goalwithproject.domain.user.QUser.user;

@RequiredArgsConstructor
public class PeerShipRepositoryImpl implements PeerShipRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    private BooleanExpression isActiveNormalUser(QUser targetUser) {
        return user.userStatus.eq(UserStatus.ACTIVE)
                .and(user.role.ne(Role.ROLE_ADMIN));
    }

    @Override
    public Page<PeerShip> searchMyPeers(Long userId, String keyword, Pageable pageable) {
        QPeerShip peerShip = QPeerShip.peerShip;

        // 복잡한 조건: (조건 A) OR (조건 B)
        BooleanBuilder builder = new BooleanBuilder();

        // 기본 조건: 상태는 수락됨(ACCEPTED)이어야 함
        builder.and(peerShip.status.eq(PeerStatus.ACCEPTED));

        // 내가 보낸 사람이거나 받은 사람 분류

        BooleanExpression case1 = peerShip.requester.id.eq(userId)
                .and(peerShip.addressee.nickName.containsIgnoreCase(keyword))
                .and(isActiveNormalUser(peerShip.addressee));

        BooleanExpression case2 = peerShip.addressee.id.eq(userId)
                .and(peerShip.requester.nickName.containsIgnoreCase(keyword))
                .and(isActiveNormalUser(peerShip.requester));

        builder.and(case1.or(case2));

        // 쿼리 실행
        List<PeerShip> content = queryFactory
                .selectFrom(peerShip)
                .join(peerShip.requester).fetchJoin() // N+1 방지용 페치 조인
                .join(peerShip.addressee).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 카운트 쿼리
        Long total = queryFactory
                .select(peerShip.count())
                .from(peerShip)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
