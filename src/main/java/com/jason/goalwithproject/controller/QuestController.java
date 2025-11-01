package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.domain.quest.ReactionType;
import com.jason.goalwithproject.dto.quest.*;
import com.jason.goalwithproject.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.Map;

@RestController
@RequestMapping("/api/quest")
@RequiredArgsConstructor
public class QuestController {
    private final QuestService questService;

    @GetMapping("")
    public ResponseEntity<QuestListDto> returnQuestList(@RequestHeader("Authorization") String authorization) {
        QuestListDto questListDto = questService.findQuests(authorization);
        return ResponseEntity.ok(questListDto);
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createQuest(@RequestHeader("Authorization") String authorization, @RequestBody QuestAddRequest questAddRequest) {
        Map<String, String> map = questService.createQuest(authorization, questAddRequest);
        return ResponseEntity.ok(map);
    }

    // 리액션 기능. 사실상 프론트에서 사용하지 않아 잠시 동결
    @GetMapping("/{questId}/reactions")
    public ResponseEntity<ReactionCountDto> returnReactionCount(@RequestHeader("Authorization") String authorization, @PathVariable Long questId) {
        ReactionCountDto reactionCount = questService.countReactions(authorization, questId);
        return ResponseEntity.ok(reactionCount);
    }

    // 수정, 삭제 메서드
    @PutMapping("/{questId}")
    public ResponseEntity<Map<String, String>> updateQuest(@RequestHeader("Authorization") String authorization, @PathVariable Long questId, @RequestBody QuestAddRequest questAddRequest) {
        return ResponseEntity.ok(questService.updateQuest(authorization, questId, questAddRequest)); // 수정 필요 보류
    }

    //
    @DeleteMapping("/{questId}")
    public ResponseEntity<Map<String, String>> deleteQuest(@RequestHeader("Authorization") String authorization, @PathVariable Long questId) {
        Map<String, String> resultMap = questService.deleteQuestWithQuestId(questId);
        return ResponseEntity.ok(resultMap);
    }

    // 생성한 퀘스트 완료
    @PutMapping("/complete/{questId}")
    public ResponseEntity<Void> completeQuest(@RequestHeader("Authorization") String authorization, @PathVariable Long questId) throws AccessDeniedException {
        questService.completeQuest(authorization, questId);
        return ResponseEntity.noContent().build();
    }

    // 인증 수행 컨트롤러
    @PostMapping("/verification/{questId}")
    public ResponseEntity<Void> verifyQuest(@RequestHeader("Authorization") String authorization, @PathVariable Long questId, @RequestBody CommentDto commentDto) throws AccessDeniedException {
        questService.verifyQuest(authorization, questId, commentDto);
        return ResponseEntity.noContent().build();
    }

    // 인증받을 퀘스트 목록 불러오기
    @GetMapping("/verification")
    public ResponseEntity<Page<QuestVerifyResponseDto>> getQuestVerifyWithPaging(@RequestHeader("Authorization") String authorization, @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<QuestVerifyResponseDto> result = questService.getRecommendedQuestsForVerification(authorization, pageable);
        return ResponseEntity.ok(result);

    }

    @GetMapping("/verification/peers")
    public ResponseEntity<Page<QuestVerifyResponseDto>> getPeerVerifyQuest(@RequestHeader("Authorization") String authorization, @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<QuestVerifyResponseDto> result = questService.getPeerQuestsForVerification(authorization, pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{questId}/reaction")
    public ResponseEntity<Void> addReaction(@RequestHeader("Authorization") String authorization, @PathVariable Long questId, @RequestBody ReactionRequestDto reactionRequestDto) {
        questService.addReaction(authorization, questId, reactionRequestDto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{questId}/reaction/{reactionType}")
    public ResponseEntity<Void> deleteReaction(@RequestHeader("Authorization") String authorization, @PathVariable Long questId, @PathVariable String reactionType) {
        questService.deleteReaction(authorization, questId, reactionType);
        return ResponseEntity.noContent().build();
    }

    // 내가 인증해줬던 (내가 작성한 댓글들이 있는) 퀘스트들 불러오기
    @GetMapping("/myVerification")
    public ResponseEntity<Page<QuestVerifyResponseDto>> getMyVerificationQuests(@RequestHeader("Authorization") String authorization, @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<QuestVerifyResponseDto> result = questService.getMyVerify(authorization, pageable);
        return ResponseEntity.ok(result);
    }

    // 내가 리액션을 남긴적이 있는 퀘스트들 불러오기
    @GetMapping("/myReaction")
    public ResponseEntity<Page<QuestVerifyResponseDto>> getMyReactionQuests(@RequestHeader("Authorization") String authorization, @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<QuestVerifyResponseDto> result = questService.getMyReaction(authorization, pageable);
        return ResponseEntity.ok(result);

    }



}
