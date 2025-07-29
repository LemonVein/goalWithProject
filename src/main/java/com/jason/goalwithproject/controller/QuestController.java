package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.domain.quest.ReactionType;
import com.jason.goalwithproject.dto.quest.QuestAddRequest;
import com.jason.goalwithproject.dto.quest.QuestDto;
import com.jason.goalwithproject.dto.quest.QuestVerifyResponseDto;
import com.jason.goalwithproject.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quest")
@RequiredArgsConstructor
public class QuestController {
    private final QuestService questService;

    @GetMapping("")
    public ResponseEntity<QuestDto> returnQuestList(@RequestHeader("Authorization") String authorization) {
        QuestDto questDto = questService.findQuests(authorization);
        return ResponseEntity.ok(questDto);
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

    @DeleteMapping("/{questId}")
    public ResponseEntity<Map<String, String>> deleteQuest(@RequestHeader("Authorization") String authorization, @PathVariable Long questId) {
        Map<String, String> resultMap = questService.deleteQuestWithQuestId(questId);
        return ResponseEntity.ok(resultMap);
    }

//    @GetMapping("/verification")
//    public ResponseEntity<Page<QuestVerifyResponseDto>> getQuestVerifyWithPaging(@RequestParam(defaultValue = "0") int page) {
//
//    }




}
