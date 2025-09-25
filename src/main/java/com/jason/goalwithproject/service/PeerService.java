package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.JwtTokenProvider;
import com.jason.goalwithproject.domain.quest.Quest;
import com.jason.goalwithproject.domain.quest.QuestStatus;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.peer.RequesterDto;
import com.jason.goalwithproject.dto.quest.QuestVerifyResponseDto;
import com.jason.goalwithproject.dto.quest.QuestWithScore;
import com.jason.goalwithproject.dto.user.UserWithScore;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeerService {
    private final PeerShipRepository peerShipRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserCharacterRepository userCharacterRepository;
    private final DtoConverterService dtoConverterService;
    private final JwtService jwtService;

    // peer 요청 로직
    public Map<String, String> requestPeer(String authorization, Long peerId) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Optional<User> requester = userRepository.findById(userId);
        Optional<User> targetUser = userRepository.findById(peerId);

        if (requester.isPresent() && targetUser.isPresent()) {
            PeerShip peerShip = new PeerShip();

            peerShip.setRequester(requester.get());
            peerShip.setAddressee(targetUser.get());
            peerShip.setStatus(PeerStatus.PENDING);

            peerShipRepository.save(peerShip);
            return Map.of("status", "success");
        }
        return Map.of("status", "failed");
    }

    // 동료 불러오기 메서드
    @Transactional(readOnly = true)
    public Page<RequesterDto> getMyPeers(String authorization, Pageable pageable) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Page<PeerShip> peerShipPage = peerShipRepository.findMyPeers(
                userId, PeerStatus.ACCEPTED, pageable);

        return peerShipPage.map(peerShip -> {
            User peerUser;
            if (peerShip.getRequester().getId().equals(userId)) {
                // 내가 요청자이면, 상대방은 수신자
                peerUser = peerShip.getAddressee();
            } else {
                // 내가 수신자이면, 상대방은 요청자
                peerUser = peerShip.getRequester();
            }

            UserCharacter userCharacter = userCharacterRepository.findByUser_IdAndIsEquippedTrue(peerUser.getId()).get();
            String characterImageUrl = (userCharacter != null && userCharacter.getCharacterImage() != null)
                    ? userCharacter.getCharacterImage().getImage()
                    : null;

            return RequesterDto.builder()
                    .id(peerUser.getId())
                    .name(peerUser.getNickName())
                    .character(characterImageUrl)
                    .userType(peerUser.getUserType().getName())
                    .level(peerUser.getLevel())
                    .build();
        });
    }

    // 동료 신청자 불러오는 메서드
    @Transactional(readOnly = true)
    public Page<RequesterDto> getRequesters(@RequestHeader String authorization, Pageable pageable) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Page<PeerShip> requestPage = peerShipRepository.findByAddressee_IdAndStatus(
                userId, PeerStatus.PENDING, pageable);

        return requestPage.map(peerShip -> {
            User requester = peerShip.getRequester(); // 요청을 보낸 사람(requester)의 정보를 가져옵니다.

            UserCharacter userCharacter = userCharacterRepository.findByUser_IdAndIsEquippedTrue(requester.getId()).get();
            String characterImageUrl = (userCharacter != null && userCharacter.getCharacterImage() != null)
                    ? userCharacter.getCharacterImage().getImage()
                    : null;

            return RequesterDto.builder()
                    .id(requester.getId())
                    .name(requester.getNickName())
                    .character(characterImageUrl)
                    .userType(requester.getUserType().getName())
                    .level(requester.getLevel())
                    .build();
        });
    }

    // 동료 수락
    public Map<String, String> acceptPeerShip(String authorization, Long requesterId) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Optional<PeerShip> targetPeerShip = peerShipRepository.findByAddressee_IdAndRequester_IdAndStatus(
                userId, requesterId, PeerStatus.PENDING
        );

        if (targetPeerShip.isPresent()) {
            PeerShip peerShip = targetPeerShip.get();
            peerShip.setStatus(PeerStatus.ACCEPTED);
            peerShipRepository.save(peerShip);
            return Map.of("status", "success");
        }
        return Map.of("status", "failed");
    }

    // 동료 거절
    public Map<String, String> rejectPeerShip(String authorization, Long requesterId) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Optional<PeerShip> targetPeerShip = peerShipRepository.findByAddressee_IdAndRequester_IdAndStatus(
                userId, requesterId, PeerStatus.PENDING
        );

        if (targetPeerShip.isPresent()) {
            PeerShip peerShip = targetPeerShip.get();
            peerShip.setStatus(PeerStatus.REJECTED);
            peerShipRepository.save(peerShip);
            return Map.of("status", "success");
        }
        return Map.of("status", "failed");
    }

    // 동료 신청 현황을 불러오는 메서드
    @Transactional(readOnly = true)
    public Page<RequesterDto> getMyPeerRequests(@RequestHeader String authorization, Pageable pageable) {
        Claims claims = jwtService.extractClaimsFromAuthorizationHeader(authorization);
        Long userId = Long.valueOf(claims.get("userId").toString());

        Page<PeerShip> requestPage = peerShipRepository.findByRequester_IdAndStatus(
                userId, PeerStatus.PENDING, pageable);

        return requestPage.map(peerShip -> {
            User addressee = peerShip.getAddressee(); // 요청을 받은 사람(addressee)의 정보를 가져온다.

            UserCharacter userCharacter = userCharacterRepository.findByUser_IdAndIsEquippedTrue(addressee.getId()).get();
            String characterImageUrl = (userCharacter != null && userCharacter.getCharacterImage() != null)
                    ? userCharacter.getCharacterImage().getImage()
                    : null;

            return RequesterDto.builder()
                    .id(addressee.getId())
                    .name(addressee.getNickName())
                    .character(characterImageUrl)
                    .userType(addressee.getUserType().getName())
                    .level(addressee.getLevel())
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public Page<RequesterDto> getRecommendedUsers(String authorization, Pageable pageable) {
        Long currentUserId = jwtService.UserIdFromToken(authorization);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        List<User> fullUsers = userRepository.findAllByUserType(currentUser.getUserType());

        // 현재 사용자의 모든 '친구' 관계를 조회합니다. (ACCEPTED 상태)
        List<PeerShip> myPeers = peerShipRepository.findMyPeers(currentUserId, PeerStatus.ACCEPTED);

        // 조회된 친구 관계에서 상대방의 ID만 추출하여 Set으로 만듭니다.
        Set<Long> friendIdSet = myPeers.stream()
                .map(peerShip -> peerShip.getRequester().getId().equals(currentUserId)
                        ? peerShip.getAddressee().getId()
                        : peerShip.getRequester().getId())
                .collect(Collectors.toSet());


        // 각 유저의 '추천 점수'를 계산하고, 유저와 점수를 함께 저장합니다.
        List<UserWithScore> scoredUsers = fullUsers.stream()
                .filter(user -> {
                    Long userId = user.getId();
                    // 본인이 아니고, 친구 목록에도 없어야 true를 반환
                    return !userId.equals(currentUserId) && !friendIdSet.contains(userId);
                })
                .map(user -> {
                    double score = calculateRecommendationScore(currentUser, user);
                    return new UserWithScore(user, score);
                })
                .collect(Collectors.toList());

        // 추천 점수가 높은 순서대로 정렬합니다.
        scoredUsers.sort(Comparator.comparingDouble(UserWithScore::getScore).reversed());

        // 정렬된 리스트를 수동으로 페이지네이션합니다.
        int start = (int) pageable.getOffset();

        if (start >= scoredUsers.size()) {
            return Page.empty(pageable);
        }

        int end = Math.min((start + pageable.getPageSize()), scoredUsers.size());
        List<User> pagedResult = scoredUsers.subList(start, end).stream()
                .map(UserWithScore::getUser)
                .collect(Collectors.toList());

        Page<User> peerPage = new PageImpl<>(pagedResult, pageable, scoredUsers.size());

        Page<RequesterDto> result = peerPage.map(dtoConverterService::convertToRequesterDto);

        return result;
    }

    // 추천 점수 계산 헬퍼 메서드
    private double calculateRecommendationScore(User currentUser, User otherUser) {
        double score = 0;

        // 레벨 유사도 점수 (차이가 적을수록 높음, 최대 50점)
        int levelDifference = Math.abs(currentUser.getLevel() - otherUser.getLevel());
        score += Math.max(0, 50 - (levelDifference * 5));

        // 유저 타입 일치 점수
        if (currentUser.getUserType().getId() == otherUser.getUserType().getId()) {
            score += 30;
        }

        // 액션 포인트 점수 (10점당 1점)
        score += (otherUser.getActionPoint() / 10.0);

        return score;
    }
}
