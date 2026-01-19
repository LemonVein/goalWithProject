package com.jason.goalwithproject.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jason.goalwithproject.dto.quest.*;
import com.jason.goalwithproject.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
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

    // 해당 퀘스트의 레코드들 불러오기
    @GetMapping("/{questId}")
    public ResponseEntity<List<QuestRecordDto>> returnQuestRecord(@RequestHeader("Authorization") String authorization , @PathVariable Long questId) {
        List<QuestRecordDto> recordDtos = questService.getQuestRecordsWithQuestId(authorization, questId);
        return ResponseEntity.ok(recordDtos);
    }

    // 해당 퀘스트의 레코드 생성
    @PostMapping("/create/{questId}")
    public ResponseEntity<Map<String, String>> createQuestRecord(@RequestHeader("Authorization") String authorization, @PathVariable Long questId,
    @RequestPart(value = "text", required = false) String text, @RequestPart(name = "images", required = false) List<MultipartFile> images
                                                                 ) throws IOException {
        Map<String, String> result = questService.addQuestRecord(authorization, questId, text, images);
        return ResponseEntity.ok(result);
    }

    // 해당 팀의 메인 퀘스트 레코드 불러오기
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

    // 팀 레코드에 다는 코멘트
    @PostMapping("/team/verifications/{recordId}")
    public ResponseEntity<Map<String, String>> addRecordComment(@RequestHeader("Authorization") String authorization, @PathVariable("recordId") Long recordId,
                                                                @RequestBody CommentDto commentDto) {
        Map<String, String> result = questService.addRecordComment(authorization, recordId, commentDto.getComment());
        return ResponseEntity.ok(result);
    }

    // 팀 레코드 수정
    @PutMapping("/team/{recordId}")
    public ResponseEntity<Map<String, String>> editTeamRecord(@RequestHeader("Authorization") String authorization, @PathVariable("recordId") Long recordId,
                                                              @RequestPart("text") String text,
                                                              @RequestPart("existingImages") String existingimages,
                                                              @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages ) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> images = objectMapper.readValue(existingimages, new TypeReference<List<String>>() {});

        RecordUpdateDto recordUpdateDto = new RecordUpdateDto(text, images);
        Map<String, String> result = questService.updateRecord(authorization, recordId, recordUpdateDto, newImages);

        return ResponseEntity.ok(result);

    }

    // 팀 레코드 삭제
    @DeleteMapping("/team/{recordId}")
    public ResponseEntity<Map<String, String>> deleteTeamRecord(@RequestHeader("Authorization") String authorization, @PathVariable("recordId") Long recordId) throws AccessDeniedException {
        Map<String, String> result = questService.deleteRecord(authorization, recordId);
        return ResponseEntity.ok(result);
    }

    // 레코드 코멘트 수정
    @PutMapping("/team/verification/{commentId}")
    public ResponseEntity<Map<String, String>> updateComment(@RequestHeader("Authorization") String authorization, @PathVariable("commentId") Long commentId, @RequestBody CommentDto commentDto) throws AccessDeniedException {
        Map<String, String> result = questService.updateComment(authorization, commentId, commentDto.getComment());
        return ResponseEntity.ok(result);
    }

    // 레코드 코멘트 삭제
    @DeleteMapping("/team/verification/{commentId}")
    public ResponseEntity<Map<String, String>> deleteComment(@RequestHeader("Authorization") String authorization, @PathVariable("commentId") Long commentId) throws AccessDeniedException {
        Map<String, String> result = questService.deleteComment(authorization, commentId);
        return ResponseEntity.ok(result);
    }

    // 레코드 리액션 추가
    @PostMapping("/{recordId}/reaction")
    public ResponseEntity<Void> addReactionRecord(@RequestHeader("Authorization") String authorization, @RequestBody ReactionRequestDto reactionRequestDto, @PathVariable Long recordId) {
        questService.addReactionRecord(authorization, recordId, reactionRequestDto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{recordId}/reaction/{reactionType}")
    public ResponseEntity<Void> deleteReactionRecord(@RequestHeader("Authorization") String authorization, @PathVariable Long recordId, @PathVariable String reactionType) {
        questService.deleteReactionRecord(authorization, recordId, reactionType);
        return ResponseEntity.noContent().build();
    }
}
