package com.jason.goalwithproject.dto.quest;

import com.jason.goalwithproject.domain.quest.ReactionType;

import java.util.EnumMap;
import java.util.Map;

public class ReactionResponseDto {
    private int support;
    private int amazing;
    private int together;
    private int perfect;

    public Map<ReactionType, Integer> toReactionMap() {
        Map<ReactionType, Integer> map = new EnumMap<>(ReactionType.class);
        map.put(ReactionType.SUPPORT, support);
        map.put(ReactionType.AMAZING, amazing);
        map.put(ReactionType.TOGETHER, together);
        map.put(ReactionType.PERFECT, perfect);
        return map;
    }
}
