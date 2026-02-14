package com.jason.goalwithproject.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeerShipRepository extends JpaRepository<PeerShip, Long>, PeerShipRepositoryCustom {
    // 26. 2. 14 권한, 유저 상태 포함
    // 받은 요청 목록
    @Query("SELECT p FROM PeerShip p " +
            "WHERE p.addressee.id = :addresseeId " +
            "AND p.status = :status " +
            "AND p.requester.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE " +
            "AND p.requester.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN")
    Page<PeerShip> findByAddressee_IdAndStatus(
            @Param("addresseeId") Long addresseeId,
            @Param("status") PeerStatus status,
            Pageable pageable);

    // 보낸 요청 목록
    @Query("SELECT p FROM PeerShip p " +
            "WHERE p.requester.id = :requesterId " +
            "AND p.status = :status " +
            "AND p.addressee.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE " +
            "AND p.addressee.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN")
    Page<PeerShip> findByRequester_IdAndStatus(
            @Param("requesterId") Long requesterId,
            @Param("status") PeerStatus status,
            Pageable pageable);

    // 내 친구 목록
    @Query("SELECT p FROM PeerShip p WHERE p.status = :status AND " +
            "(" +
            "   (p.requester.id = :userId AND p.addressee.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE AND p.addressee.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN) " +
            "   OR " +
            "   (p.addressee.id = :userId AND p.requester.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE AND p.requester.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN)" +
            ")")
    Page<PeerShip> findMyPeers(
            @Param("userId") Long userId,
            @Param("status") PeerStatus status,
            Pageable pageable);

    // 리스트 전용 내 친구 목록
    @Query("SELECT p FROM PeerShip p WHERE p.status = :status AND " +
            "(" +
            "   (p.requester.id = :userId AND p.addressee.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE AND p.addressee.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN) " +
            "   OR " +
            "   (p.addressee.id = :userId AND p.requester.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE AND p.requester.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN)" +
            ")")
    List<PeerShip> findMyPeers(
            @Param("userId") Long userId,
            @Param("status") PeerStatus status);

    // 로직 체크 검증용 메서드
    Optional<PeerShip> findByAddressee_IdAndRequester_IdAndStatus(Long addresseeId, Long requesterId, PeerStatus status);
    List<PeerShip> findByAddressee_IdAndStatus(Long addresseeId, PeerStatus status);

    @Query("SELECT p FROM PeerShip p WHERE (p.requester.id = :user1Id AND p.addressee.id = :user2Id) OR (p.requester.id = :user2Id AND p.addressee.id = :user1Id)")
    List<PeerShip> findAnyRelationship(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    // 신청자 아이디, 받은 사람 아이디, 그리고 상태
    Optional<PeerShip> findByRequester_IdAndAddressee_IdAndStatus(Long requesterId, Long addresseeId, PeerStatus status);

    // 동료 검색 중, 내 키워드로 검색
    @Query("SELECT p FROM PeerShip p " +
            "WHERE p.requester.id = :requesterId " +
            "AND p.status = :status " +
            "AND p.addressee.nickName LIKE %:nickname% " +
            "AND p.addressee.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE " +
            "AND p.addressee.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN")
    Page<PeerShip> findByRequester_IdAndStatusAndAddressee_NickNameContaining(
            @Param("requesterId") Long requesterId,
            @Param("status") PeerStatus status,
            @Param("nickname") String nickname,
            Pageable pageable
    );

    // 나에게 온 요청 중, 보낸 사람 닉네임으로 검색
    @Query("SELECT p FROM PeerShip p " +
            "WHERE p.addressee.id = :addresseeId " +
            "AND p.status = :status " +
            "AND p.requester.nickName LIKE %:nickname% " +
            "AND p.requester.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE " +
            "AND p.requester.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN")
    Page<PeerShip> findByAddressee_IdAndStatusAndRequester_NickNameContaining(
            @Param("addresseeId") Long addresseeId,
            @Param("status") PeerStatus status,
            @Param("nickname") String nickname,
            Pageable pageable
    );

    // 동료 맺은 수 카운트
    @Query("SELECT COUNT(p) FROM PeerShip p WHERE p.status = com.jason.goalwithproject.domain.user.PeerStatus.ACCEPTED AND " +
            "(" +
            "   (p.requester.id = :userId " +
            "    AND p.addressee.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE " +
            "    AND p.addressee.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN) " +
            "   OR " +
            "   (p.addressee.id = :userId " +
            "    AND p.requester.userStatus = com.jason.goalwithproject.domain.user.UserStatus.ACTIVE " +
            "    AND p.requester.role <> com.jason.goalwithproject.domain.user.Role.ROLE_ADMIN)" +
            ")")
    long countAcceptedPeersByUserId(@Param("userId") Long userId);
}
