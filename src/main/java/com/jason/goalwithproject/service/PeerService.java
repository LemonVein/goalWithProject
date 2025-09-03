package com.jason.goalwithproject.service;

import com.jason.goalwithproject.config.JwtTokenProvider;
import com.jason.goalwithproject.domain.user.*;
import com.jason.goalwithproject.dto.peer.RequesterDto;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PeerService {
    private final PeerShipRepository peerShipRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserCharacterRepository userCharacterRepository;
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

            UserCharacter userCharacter = userCharacterRepository.findByUser_Id(peerUser.getId());
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

            UserCharacter userCharacter = userCharacterRepository.findByUser_Id(requester.getId());
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
            User addressee = peerShip.getAddressee(); // 요청을 받은 사람(addressee)의 정보를 가져옵니다.

            UserCharacter userCharacter = userCharacterRepository.findByUser_Id(addressee.getId());
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
}
