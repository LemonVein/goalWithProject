package com.jason.goalwithproject.dto.quest;

import com.jason.goalwithproject.domain.quest.ReactionType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class ReactionCountDto {
    private int support;
    private int amazing;
    private int together;
    private int perfect;
    private Map<ReactionType, Boolean> myReaction;
}
