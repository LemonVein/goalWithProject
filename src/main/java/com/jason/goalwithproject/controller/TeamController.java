package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.dto.team.TeamAddRequestDto;
import com.jason.goalwithproject.dto.team.TeamResponseDto;
import com.jason.goalwithproject.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.sql.In;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
public class TeamController {
    private final TeamService teamService;

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getUserTeams(@RequestHeader("Authorization") String authorization) {
        List<TeamResponseDto> UserTeams = teamService.findAllUserTeams(authorization);
        return ResponseEntity.ok(Map.of("teams", UserTeams));
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Integer>> createTeam(@RequestHeader("Authorization") String authorization, @RequestBody TeamAddRequestDto teamAddRequestDto) {
        Map<String, Integer> result = teamService.createTeam(authorization, teamAddRequestDto);
        return ResponseEntity.ok(result);
    }

    // 완성하지 않음 수정 필요함
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Map<String, Object>> deleteTeam(@PathVariable("teamId") Long teamId) {
        return ResponseEntity.ok(Map.of("status", "success")); // 수정 필요 보류
    }

}
