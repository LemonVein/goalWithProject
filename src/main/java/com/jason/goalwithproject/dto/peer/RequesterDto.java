package com.jason.goalwithproject.dto.peer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RequesterDto {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("nickname")
    private String name;

    @JsonProperty("character")
    private String character;

    @JsonProperty("userType")
    private String userType;

    @JsonProperty("level")
    private int level;

}
