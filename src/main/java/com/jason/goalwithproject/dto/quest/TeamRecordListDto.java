package com.jason.goalwithproject.dto.quest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class TeamRecordListDto {
    private List<TeamQuestRecordDto> records;
}
