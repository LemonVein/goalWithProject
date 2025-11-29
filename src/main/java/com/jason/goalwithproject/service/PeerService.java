package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.JwtTokenProvider;
import com.jason.goalwithproject.domain.custom.CharacterImage;
import com.jason.goalwithproject.domain.custom.CharacterImageRepository;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.peer.RequesterDto;
import com.jason.goalwithproject.dto.user.UserWithScore;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeerService {
    private final PeerShipRepository peerShipRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserCharacterRepository userCharacterRepository;
    private final CharacterImageRepository characterImageRepository;
    private final DtoConverterService dtoConverterService;
    private final JwtService jwtService;

    // peer 요청 로직
    @Transactional
    public Map<String, String> requestPeer(String authorization, Long peerId) {
        Long userId = jwtService.UserIdFromToken(authorization);

        // 자기 자신에게 요청하는지 확인
        if (userId.equals(peerId)) {
            throw new IllegalArgumentException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }

        // 두 사용자 (requester, targetUser)가 존재하는지 확인
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("요청자를 찾을 수 없습니다."));
        User targetUser = userRepository.findById(peerId)
                .orElseThrow(() -> new EntityNotFoundException("대상을 찾을 수 없습니다."));

        // 기존 관계가 있는지 확인
        List<PeerShip> existingRelationships = peerShipRepository.findAnyRelationship(userId, peerId);

        if (!existingRelationships.isEmpty()) {
            // 기존 관계의 상태 확인
            for (PeerShip ship : existingRelationships) {
                if (ship.getStatus() == PeerStatus.ACCEPTED) {
                    throw new IllegalArgumentException("이미 친구 관계입니다.");
                }
                if (ship.getStatus() == PeerStatus.PENDING) {
                    throw new IllegalArgumentException("이미 친구 요청을 보냈거나 받은 상태입니다.");
                }
            }
        }

        // 새 요청 생성
        PeerShip peerShip = new PeerShip();
        peerShip.setRequester(requester);
        peerShip.setAddressee(targetUser);
        peerShip.setStatus(PeerStatus.PENDING); //

        peerShipRepository.save(peerShip);
        return Map.of("status", "success");
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

            // 도전과제 체크
            checkFirstPeerAchievement(peerShip.getRequester());
            checkFirstPeerAchievement(peerShip.getAddressee());
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

    public List<Long> getMyPeerIds(String authorization) {
        Long currentUserId = jwtService.UserIdFromToken(authorization);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        List<PeerShip> myPeers = peerShipRepository.findMyPeers(currentUserId, PeerStatus.ACCEPTED);

        return myPeers.stream()
                .map(peerShip -> {
                    // '나'가 아닌 '상대방(동료)'이 누구인지 확인하여 그 ID를 반환합니다.
                    if (peerShip.getRequester().getId().equals(currentUserId)) {
                        return peerShip.getAddressee().getId();
                    } else {
                        return peerShip.getRequester().getId();
                    }
                })
                .collect(Collectors.toList());

    }

    // 유저에 맞는 추천 유저를 추천해주는 메서드
    @Transactional(readOnly = true)
    public Page<RequesterDto> getRecommendedUsers(String authorization, Pageable pageable) {
        Long currentUserId = jwtService.UserIdFromToken(authorization);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        int level = currentUser.getLevel();
        int minLevel = Math.max(1, level - 5);
        int maxLevel = level + 5;

        // DB에서 가져올 후보군을 200명으로 제한 (최신 가입자 순)
        Pageable candidatePageable = PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id"));

        // 레벨 범위에 맞는 사용자들을 후보군으로 조회합니다. (본인 제외)
        Page<User> candidatePage = userRepository.findByLevelBetweenAndIdNot(
                minLevel, maxLevel, currentUserId, candidatePageable);
        List<User> fullUsers = candidatePage.getContent();

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

    @Transactional
    public void cancelPeerRequest(String authorization, Long addresseeId) {
        Long userId = jwtService.UserIdFromToken(authorization);

        PeerShip peerShipToCancel = peerShipRepository.findByRequester_IdAndAddressee_IdAndStatus(userId, addresseeId, PeerStatus.PENDING)
                .orElseThrow(() -> new EntityNotFoundException("취소할 수 있는 친구 요청이 없습니다"));

        peerShipRepository.delete(peerShipToCancel);
    }

    // 헬로 피코 지급 메서드
    private void checkFirstPeerAchievement(User user) {
        // 헬로 피코
        final int REWARD_CHARACTER_ID = 5;

        // 1. 이미 해당 캐릭터를 가지고 있는지 확인 (중복 지급 방지)
        boolean alreadyHas = userCharacterRepository.existsByUser_IdAndCharacterImage_Id(
                user.getId(), REWARD_CHARACTER_ID);

        if (alreadyHas) {
            return;
        }

        // 현재까지 'ACCEPTED' 상태인 동료 관계 개수 조회
        long acceptedPeerCount = peerShipRepository.countAcceptedPeersByUserId(user.getId());

        if (acceptedPeerCount == 1) {

            // 캐릭터 지급
            CharacterImage rewardCharacter = characterImageRepository.findById(REWARD_CHARACTER_ID);

            if (rewardCharacter != null) {
                UserCharacter newUserCharacter = new UserCharacter();
                newUserCharacter.setUser(user);
                newUserCharacter.setCharacterImage(rewardCharacter);
                newUserCharacter.setEquipped(false); // 지급만 하고 장착은 안 함
                userCharacterRepository.save(newUserCharacter);

                log.info("ACHIEVEMENT UNLOCKED: User {} 님이 첫 퀘스트 완료 보상으로 캐릭터({})를 획득했습니다.", user.getId(), rewardCharacter.getName());
            }
        }
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
