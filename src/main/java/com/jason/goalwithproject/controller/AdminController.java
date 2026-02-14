package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.dto.quest.QuestDto;
import com.jason.goalwithproject.dto.quest.QuestVerifyDto;
import com.jason.goalwithproject.dto.user.UserInfoForAdmin;
import com.jason.goalwithproject.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 모든 유저의 퀘스트 불러오기
    @GetMapping("/users")
    public ResponseEntity<Page<UserInfoForAdmin>> getUsers(Pageable pageable) {
        Page<UserInfoForAdmin> userPage = adminService.getAllUsersForAdmin(pageable);
        return ResponseEntity.ok(userPage);
    }

    // 모든 퀘스트 가져오기
    @GetMapping("/quests")
    public ResponseEntity<Page<QuestDto>> getQuests(Pageable pageable) {
        Page<QuestDto> questPage = adminService.getAllQuestsForAdmin(pageable);
        return ResponseEntity.ok(questPage);
    }

    // 단일 퀘스트 정보 불러오기
    @GetMapping("/quest/{id}")
    public ResponseEntity<QuestVerifyDto> getQuestById(@PathVariable Long id) {
        QuestVerifyDto questVerifyDto = adminService.getQuestById(id);
        return ResponseEntity.ok(questVerifyDto);
    }

    // 유저 정지 시키기
    @PutMapping("/user/inactive/{id}")
    public ResponseEntity<Void> inactiveUser(@PathVariable Long id) {
        adminService.suspendUser(id);
        return ResponseEntity.noContent().build();
    }

    // 유저 삭제 (논리적 삭제, 사실상 탈퇴시키기)
    @DeleteMapping("/user/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // 퀘스트 삭제
    @DeleteMapping("/quest/{id}")
    public ResponseEntity<Void> deleteQuest(@PathVariable Long id) {
        adminService.deleteQuest(id);
        return ResponseEntity.noContent().build();
    }

    // 레코드 삭제
    @DeleteMapping("/quest/record/{id}")
    public ResponseEntity<Void> deleteQuestRecord(@PathVariable Long id) {
        adminService.deleteQuestRecord(id);
        return ResponseEntity.noContent().build();
    }

    // 댓글 삭제
    @DeleteMapping("/quest/verification/{id}")
    public ResponseEntity<Void> deleteQuestVerification(@PathVariable Long id) {
        adminService.deleteVerification(id);
        return ResponseEntity.noContent().build();
    }




}
