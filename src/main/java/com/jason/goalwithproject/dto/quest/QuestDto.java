package com.jason.goalwithproject.dto.quest;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestDto {
    List<QuestResponseDto> quests;
}
