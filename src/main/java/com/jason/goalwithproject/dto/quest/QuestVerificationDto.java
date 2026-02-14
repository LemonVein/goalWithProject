package com.jason.goalwithproject.dto.quest;

import com.jason.goalwithproject.domain.quest.QuestVerification;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestVerificationDto {
    private Long userId;
    private String comment;

    public static QuestVerificationDto fromEntity(QuestVerification questVerification) {
        QuestVerificationDto dto = new QuestVerificationDto();
        if (questVerification.getUser() != null) {
            dto.setUserId(questVerification.getUser().getId());
        } else {
            dto.setUserId(null); // 또는 -1L 같은 특정 값으로 구분 가능
        }
        dto.setComment(questVerification.getComment());
        return dto;
    }
}
