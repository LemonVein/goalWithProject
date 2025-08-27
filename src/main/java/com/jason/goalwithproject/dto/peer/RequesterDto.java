package com.jason.goalwithproject.dto.peer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RequesterDto {
    @JsonProperty("requester_id")
    private Long id;
    @JsonProperty("requester_name")
    private String name;
    @JsonProperty("requester_character")
    private String character;
    @JsonProperty("requester_userType")
    private String userType;
    @JsonProperty("requester_level")
    private int level;

}
