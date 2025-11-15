package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.dto.peer.RequesterDto;
import com.jason.goalwithproject.service.PeerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/peer")
@RequiredArgsConstructor
public class PeerController {
    private final PeerService peerService;

    // 내 동료들 확인하기
    @GetMapping("")
    public ResponseEntity<Page<RequesterDto>> getMyPeers(@RequestHeader("Authorization") String authorization, @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RequesterDto> peers = peerService.getMyPeers(authorization, pageable);
        return ResponseEntity.ok(peers);
    }

    // 동료 요청
    @PostMapping("/{userId}")
    public ResponseEntity<Map<String, String>> requestPeer(@RequestHeader("Authorization") String authorization, @PathVariable Long userId) {
        Map<String, String> result = peerService.requestPeer(authorization, userId);
        return ResponseEntity.ok(result);
    }

    // 동료 요청 취소
    @DeleteMapping("/requesting/{userId}")
    public ResponseEntity<Void> cancelRequesting(@RequestHeader("Authorization") String authorization, @PathVariable Long userId) {
        peerService.cancelPeerRequest(authorization, userId);
        return ResponseEntity.noContent().build();
    }

    // 받은 동료 요청들 불러오기
    @GetMapping("/requested")
    public ResponseEntity<Page<RequesterDto>> getRequesters(@RequestHeader("Authorization") String authorization,
                                                            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RequesterDto> result = peerService.getRequesters(authorization, pageable);
        return ResponseEntity.ok(result);
    }

    // 받은 요청 수락하기
    @PostMapping("/accept/{userId}")
    public ResponseEntity<Map<String, String>> acceptPeer(@RequestHeader("Authorization") String authorization, @PathVariable Long userId) {
        Map<String, String> result = peerService.acceptPeerShip(authorization, userId);
        return ResponseEntity.ok(result);
    }

    // 받은 요청 거절하기
    @PostMapping("/reject/{userId}")
    public ResponseEntity<Map<String, String>> rejectPeer(@RequestHeader("Authorization") String authorization, @PathVariable Long userId) {
        Map<String, String> result = peerService.rejectPeerShip(authorization, userId);
        return ResponseEntity.ok(result);
    }

    // 내가 요청한 동료요청 확인하기
    @GetMapping("/requesting")
    public ResponseEntity<Page<RequesterDto>> getMyPeerRequests(@RequestHeader("Authorization") String authorization,
                                                                @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RequesterDto> result = peerService.getMyPeerRequests(authorization, pageable);
        return ResponseEntity.ok(result);
    }

    // 추천 유저 불러오기
    @GetMapping("/recommend")
    public ResponseEntity<Page<RequesterDto>> getRecommendedUsers(@RequestHeader("Authorization") String authorization, Pageable pageable) {
        Page<RequesterDto> result = peerService.getRecommendedUsers(authorization, pageable);
        return ResponseEntity.ok(result);
    }

    // 내 동료들 불러오기
    @GetMapping("/myPeerId")
    public ResponseEntity<List<Long>> getMyPeerIds(@RequestHeader("Authorization") String authorization) {
        List<Long> result = peerService.getMyPeerIds(authorization);
        return ResponseEntity.ok(result);
    }
}
