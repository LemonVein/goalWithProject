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
    Page<PeerShip> findByAddressee_IdAndStatus(Long addresseeId, PeerStatus status, Pageable pageable);
    Page<PeerShip> findByRequester_IdAndStatus(Long requesterId, PeerStatus status, Pageable pageable);
    @Query("SELECT p FROM PeerShip p WHERE (p.requester.id = :userId OR p.addressee.id = :userId) AND p.status = :status")
    Page<PeerShip> findMyPeers(
            @Param("userId") Long userId,
            @Param("status") PeerStatus status,
            Pageable pageable);
    @Query("SELECT p FROM PeerShip p WHERE (p.requester.id = :userId OR p.addressee.id = :userId) AND p.status = :status")
    List<PeerShip> findMyPeers(
            @Param("userId") Long userId,
            @Param("status") PeerStatus status
            );
    Optional<PeerShip> findByAddressee_IdAndRequester_IdAndStatus(Long addresseeId, Long requesterId, PeerStatus status);
    List<PeerShip> findByAddressee_IdAndStatus(Long addresseeId, PeerStatus status);

    @Query("SELECT p FROM PeerShip p WHERE (p.requester.id = :user1Id AND p.addressee.id = :user2Id) OR (p.requester.id = :user2Id AND p.addressee.id = :user1Id)")
    List<PeerShip> findAnyRelationship(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    // 신청자 아이디, 받은 사람 아이디, 그리고 상태
    Optional<PeerShip> findByRequester_IdAndAddressee_IdAndStatus(Long requesterId, Long addresseeId, PeerStatus status);

    // 동료 검색 중, 내 키워드로 검색
    Page<PeerShip> findByRequester_IdAndStatusAndAddressee_NickNameContaining(
            Long requesterId,
            PeerStatus status,
            String nickname,
            Pageable pageable
    );

    // 나에게 온 요청 중, 보낸 사람 닉네임으로 검색
    Page<PeerShip> findByAddressee_IdAndStatusAndRequester_NickNameContaining(
            Long addresseeId,
            PeerStatus status,
            String nickname,
            Pageable pageable
    );

    // 동료 맺은 수 카운트
    @Query("SELECT COUNT(ps) FROM PeerShip ps " +
            "WHERE (ps.requester.id = :userId OR ps.addressee.id = :userId) " +
            "AND ps.status = com.jason.goalwithproject.domain.user.PeerStatus.ACCEPTED")
    long countAcceptedPeersByUserId(@Param("userId") Long userId);
}
