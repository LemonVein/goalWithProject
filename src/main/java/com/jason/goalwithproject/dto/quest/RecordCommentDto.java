package com.jason.goalwithproject.dto.quest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jason.goalwithproject.domain.quest.QuestVerification;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class RecordCommentDto {
    private Long id;
    @JsonProperty("user_id")
    private Long userId;
    private String username;
    private String comment;
    private String character;
    private LocalDateTime createdAt;
    private Long replyCount;

    public static RecordCommentDto from(QuestVerification verification, String url, Long replyCount) {
        String username;
        Long userId;
        if (verification.getUser() == null) {
            username = "Unknown";
            userId = null;
        } else {
            username = verification.getUser().getName();
            userId = verification.getUser().getId();
        }

        return RecordCommentDto.builder()
                .id(verification.getId())
                .userId(userId)
                .username(username)
                .comment(verification.getComment())
                .character(url)
                .createdAt(verification.getCreatedAt())
                .replyCount(replyCount)
                .build();
    }
}
