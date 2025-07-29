package com.jason.goalwithproject.dto.quest;

import com.jason.goalwithproject.domain.quest.QuestRecord;

import java.time.LocalDateTime;
import java.util.List;

public class QuestRecordDto {
    private Long id;
    private LocalDateTime date;
    private String text;
    private List<String> images;
    private Long questId;
    private LocalDateTime createdAt;
    private Long userId;

    public QuestRecordDto(Long id, LocalDateTime date, String text, List<String> images, Long id1, LocalDateTime createdAt, Long userId) {
    }

    public static QuestRecordDto fromEntity(QuestRecord record, List<String> images, Long userId) {
        return new QuestRecordDto(
                record.getId(),
                record.getDate(),
                record.getText(),
                images,
                record.getQuest().getId(),
                record.getCreatedAt(),
                userId
        );
    }
}
