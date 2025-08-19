package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.dto.quest.CommentDto;
import com.jason.goalwithproject.dto.quest.QuestRecordDto;
import com.jason.goalwithproject.dto.quest.RecordAddRequest;
import com.jason.goalwithproject.dto.quest.TeamQuestRecordDto;
import com.jason.goalwithproject.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/record")
@RequiredArgsConstructor
public class RecordController {
    private final QuestService questService;

    @GetMapping("/{questId}")
    public ResponseEntity<List<QuestRecordDto>> returnQuestRecord(@RequestHeader("Authorization") String authorization , @PathVariable Long questId) {
        List<QuestRecordDto> recordDtos = questService.getQuestRecordsWithQuestId(authorization, questId);
        return ResponseEntity.ok(recordDtos);
    }

    @PostMapping("/create/{questId}")
    public ResponseEntity<Map<String, String>> createQuestRecord(@RequestHeader("Authorization") String authorization, @PathVariable Long questId,
    @RequestPart("text") String text, @RequestPart(name = "images", required = false) List<MultipartFile> images
                                                                 ) throws IOException {
        Map<String, String> result = questService.addQuestRecord(authorization, questId, text, images);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<Page<TeamQuestRecordDto>> getTeamQuestRecord(@RequestHeader("Authorization") String authorization, @PathVariable("teamId") int teamId,
    @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TeamQuestRecordDto> dtos = questService.getTeamQuestRecords(authorization, teamId, pageable);
        return ResponseEntity.ok(dtos);

    }

    // 팀 레코드 추가 메서드
    @PostMapping("/team/{teamId}")
    public ResponseEntity<Map<String, String>> createTeamPost(@RequestHeader("Authorization") String authorization, @PathVariable("teamId") int teamId,
    @RequestPart("text") String text, @RequestPart(name = "images", required = false) List<MultipartFile> images){

        Map<String, String> result = questService.addQuestTeamRecord(authorization, teamId, text, images);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/team/verifications/{recordId}")
    public ResponseEntity<Map<String, String>> addRecordComment(@RequestHeader("Authorization") String authorization, @PathVariable("recordId") Long recordId,
                                                                @RequestBody CommentDto commentDto) {
        Map<String, String> result = questService.addRecordComment(authorization, recordId, commentDto.getComment());
        return ResponseEntity.ok(result);

    }
}
