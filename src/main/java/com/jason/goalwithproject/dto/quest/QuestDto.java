package com.jason.goalwithproject.dto.quest;

import com.jason.goalwithproject.domain.quest.QuestStatus;
import com.jason.goalwithproject.dto.user.UserDto;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 어드민 페이지에서 순수한 퀘스트 내용과 그 퀘스트의 주인을 알수있는 DTO
public class QuestDto {
    private Long id;
    private String title;
    private boolean isMain;
    private QuestStatus questStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private UserDto user;
}
