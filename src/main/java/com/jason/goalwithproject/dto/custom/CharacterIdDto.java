package com.jason.goalwithproject.dto.custom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CharacterIdDto {
    @JsonProperty("character")
    private Long id;
}
