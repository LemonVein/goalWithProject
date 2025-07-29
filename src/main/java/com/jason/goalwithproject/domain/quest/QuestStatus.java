package com.jason.goalwithproject.domain.quest;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum QuestStatus {
    @JsonProperty("complete")
    COMPLETE,

    @JsonProperty("progress")
    PROGRESS,

    @JsonProperty("verify")
    VERIFY,

    @JsonProperty("failed")
    FAILED
}