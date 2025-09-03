package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.domain.quest.ReactionType;
import com.jason.goalwithproject.dto.quest.QuestAddRequest;
import com.jason.goalwithproject.dto.quest.QuestListDto;
import com.jason.goalwithproject.dto.quest.QuestVerifyResponseDto;
import com.jason.goalwithproject.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @GetMapping("/{questId}/reactions")
    public ResponseEntity<Map<ReactionType, Integer>> returnReactionCount(@PathVariable Long questId) {
        Map<ReactionType, Integer> reactionMap = questService.countReactions(questId);
        return ResponseEntity.ok(reactionMap);
    }

    // 수정, 삭제 메서드
    @PutMapping("/{questId}")
    public ResponseEntity<Map<String, String>> updateQuest(@RequestHeader("Authorization") String authorization, @PathVariable Long questId, @RequestBody QuestAddRequest questAddRequest) {
        return ResponseEntity.ok(questService.updateQuest(authorization, questId, questAddRequest)); // 수정 필요 보류
    }

    @DeleteMapping("/{questId}")
    public ResponseEntity<Map<String, String>> deleteQuest(@RequestHeader("Authorization") String authorization, @PathVariable Long questId) {
        Map<String, String> resultMap = questService.deleteQuestWithQuestId(questId);
        return ResponseEntity.ok(resultMap);
    }

    // 수정 필요 퀘스트 인증 수행 컨트롤러
    @PostMapping("/verification/{questId}")
    public ResponseEntity<Map<String, String>> verifyQuest(@RequestHeader("Authorization") String authorization, @PathVariable Long questId, @RequestBody String comment) {
        return ResponseEntity.ok(Map.of("suscess", "sss"));
    }

    // 생성한 퀘스트 완료
    @PutMapping("/complete/{questId}")
    public ResponseEntity<Void> completeQuest(@RequestHeader("Authorization") String authorization, @PathVariable Long questId) throws AccessDeniedException {
        questService.completeQuest(authorization, questId);
        return ResponseEntity.noContent().build();
    }

    // 인증받을 퀘스트 목록 불러오기
//    @GetMapping("/verification")
//    public ResponseEntity<Page<QuestVerifyResponseDto>> getQuestVerifyWithPaging(@RequestParam(defaultValue = "0") int page) {
//        return throw new AccessDeniedException()
//    }




}
