package com.jason.goalwithproject.dto.quest;

import com.jason.goalwithproject.domain.quest.Quest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuestWithScore {
    private Quest quest;
    private double score;
}
