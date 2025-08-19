package com.jason.goalwithproject.dto.quest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jason.goalwithproject.dto.user.UserDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class TeamQuestRecordDto {
    private Long id;
    private String text;
    private List<String> images;
    private LocalDateTime createdAt;
    private UserDto user;
    private List<RecordCommentDto> verifications;


}
