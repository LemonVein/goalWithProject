package com.jason.goalwithproject.domain.quest;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ReactionType {
    SUPPORT,
    AMAZING,
    TOGETHER,
    PERFECT;

    @JsonValue
    public String toLower() {
        return this.name().toLowerCase();
    }
}
