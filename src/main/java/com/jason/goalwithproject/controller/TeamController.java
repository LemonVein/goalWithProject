package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.dto.team.TeamResponseDto;
import com.jason.goalwithproject.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
public class TeamController {
    private final TeamService teamService;

    @GetMapping("")
    public Map<String, Object> getUserTeams(@RequestHeader("Authorization") String authorization) {
        List<TeamResponseDto> UserTeams = teamService.findAllUserTeams(authorization);
        return Map.of("teams", UserTeams);
    }

}
