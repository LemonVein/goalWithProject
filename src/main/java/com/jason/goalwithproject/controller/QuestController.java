package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.dto.common.ReportRequestDto;
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
import java.util.List;
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
    public ResponseEntity<Map<String, String>> createQuest(@RequestHeader("Authorization") String authorization, @RequestBody QuestAddRequest questAddRequest) throws Exception {
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

    @DeleteMapping("/{questId}")
    public ResponseEntity<Map<String, String>> deleteQuest(@RequestHeader("Authorization") String authorization, @PathVariable Long questId) {
        Map<String, String> resultMap = questService.deleteQuestWithQuestId(authorization, questId);
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
    public ResponseEntity<Page<UserQuestVerifyResponseDto>> getQuestVerifyWithPaging(@RequestHeader("Authorization") String authorization,
                                                                                     @RequestParam(value = "search", required = false) String search,
                                                                                     @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserQuestVerifyResponseDto> result = questService.getRecommendedQuestsForVerification(authorization, search, pageable);
        return ResponseEntity.ok(result);

    }

    // 동료들의 인증받을 게시물들 불러오기 (search 추가 2025.12.27)
    @GetMapping("/verification/peers")
    public ResponseEntity<Page<UserQuestVerifyResponseDto>> getPeerVerifyQuest(@RequestHeader("Authorization") String authorization,
                                                                               @RequestParam(value = "search", required = false) String search,
                                                                               @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserQuestVerifyResponseDto> result = questService.getPeerQuestsForVerification(authorization, search, pageable);
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

    // 인증 댓글 수정 및 삭제 메서드들
    @PutMapping("/verifications/{verificationId}")
    public ResponseEntity<Void> editComment(@RequestHeader("Authorization") String authorization, @PathVariable Long verificationId, @RequestBody CommentDto commentDto) throws AccessDeniedException {
        questService.editVerification(authorization, verificationId, commentDto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/verifications/{verificationId}")
    public ResponseEntity<Void> deleteComment(@RequestHeader("Authorization") String authorization, @PathVariable Long verificationId) throws AccessDeniedException {
        questService.deleteComment(authorization, verificationId);
        return ResponseEntity.noContent().build();
    }

    // 내가 인증해줬던 (내가 작성한 댓글들이 있는) 퀘스트들 불러오기
    @GetMapping("/myVerification")
    public ResponseEntity<Page<UserQuestVerifyResponseDto>> getMyVerificationQuests(@RequestHeader("Authorization") String authorization, @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserQuestVerifyResponseDto> result = questService.getMyVerify(authorization, pageable);
        return ResponseEntity.ok(result);
    }

    // 내가 리액션을 남긴적이 있는 퀘스트들 불러오기
    @GetMapping("/myReaction")
    public ResponseEntity<Page<UserQuestVerifyResponseDto>> getMyReactionQuests(@RequestHeader("Authorization") String authorization, @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserQuestVerifyResponseDto> result = questService.getMyReaction(authorization, pageable);
        return ResponseEntity.ok(result);
    }

    // 북마크 추가
    @PostMapping("/{questId}/bookmark")
    public ResponseEntity<Void> addBookmark(@RequestHeader("Authorization") String authorization, @PathVariable Long questId) {
        questService.addBookmark(authorization, questId);
        return ResponseEntity.noContent().build();
    }

    // 북마크된 나의 퀘스트들을 보내준다
    @GetMapping("/bookmarked")
    public ResponseEntity<Page<UserQuestVerifyResponseDto>> getMyBookmarkedQuests(@RequestHeader("Authorization") String authorization, @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserQuestVerifyResponseDto> bookmarks = questService.getMyBookmarks(authorization, pageable);
        return ResponseEntity.ok(bookmarks);
    }

    // 퀘스트 북마크 취소
    @DeleteMapping("/{questId}/bookmark")
    public ResponseEntity<Void> deleteBookmark(@RequestHeader("Authorization") String authorization, @PathVariable Long questId) {
        questService.cancelBookmark(authorization, questId);
        return ResponseEntity.noContent().build();

    }

    // 단일 퀘스트의 요약 정보를 보내준다
    @GetMapping("verification/{questId}")
    public ResponseEntity<QuestSummationDto> getQuestSummation(@RequestHeader("Authorization") String authorization, @PathVariable Long questId) {
        QuestSummationDto result = questService.getQuestSummation(authorization, questId);
        return ResponseEntity.ok(result);
    }

    // 특정 댓글의 대댓글 불러오기
    @GetMapping("verification/comment/{verificationId}")
    public ResponseEntity<List<RecordCommentDto>> getReplies(@RequestHeader("Authorization") String authorization, @PathVariable Long verificationId) {
        List<RecordCommentDto> replies = questService.getReplies(authorization, verificationId);
        return ResponseEntity.ok(replies);
    }

    // 대댓글 작성
    @PostMapping("/verification/comment/{verificationId}")
    public ResponseEntity<Void> addReply(@RequestHeader("Authorization") String authorization, @PathVariable Long verificationId, @RequestBody CommentDto commentDto) {
        questService.addReply(authorization, verificationId, commentDto);
        return ResponseEntity.noContent().build();
    }

    // 퀘스트 신고
    @PostMapping("/report/{id}")
    public ResponseEntity<Void> reportQuset(@RequestHeader("Authorization") String authorization, @PathVariable Long id, @RequestBody ReportRequestDto reportRequestDto) throws AccessDeniedException {
        questService.reportQuest(authorization, id, reportRequestDto);
        return ResponseEntity.noContent().build();
    }

    // 댓글 신고
    @PostMapping("verification/report/{id}")
    public ResponseEntity<Void> reportVerification(@RequestHeader("Authorization") String authorization, @PathVariable Long id, @RequestBody ReportRequestDto reportRequestDto) throws AccessDeniedException {
        questService.reportVerification(authorization, id, reportRequestDto);
        return ResponseEntity.noContent().build();
    }



}
