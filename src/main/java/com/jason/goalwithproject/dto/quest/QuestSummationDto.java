package com.jason.goalwithproject.dto.quest;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class QuestSummationDto {
    private Long id;
    private String title;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<QuestRecordDto> records;
    private List<RecordCommentDto> verifications;
}
