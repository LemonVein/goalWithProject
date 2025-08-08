package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.dto.quest.QuestRecordDto;
import com.jason.goalwithproject.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    public ResponseEntity<Map<String, String>> createQuestRecord(@RequestHeader("Authorization") String authorization , @PathVariable Long questId,
    @RequestPart("text") String text, @RequestPart(name = "images", required = false) List<MultipartFile> images
                                                                 ) throws IOException {
        Map<String, String> result = questService.addQuestRecord(authorization, questId, text, images);
        return ResponseEntity.ok(result);

    }
}
