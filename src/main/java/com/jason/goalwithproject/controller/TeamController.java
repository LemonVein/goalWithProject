package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.dto.team.TeamAddRequestDto;
import com.jason.goalwithproject.dto.team.TeamResponseDto;
import com.jason.goalwithproject.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
public class TeamController {
    private final TeamService teamService;

    // 팀 불러오기
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getUserTeams(@RequestHeader("Authorization") String authorization) {
        List<TeamResponseDto> UserTeams = teamService.findAllUserTeams(authorization);
        return ResponseEntity.ok(Map.of("teams", UserTeams));
    }

    // 팀 생성
    @PostMapping("/create")
    public ResponseEntity<Map<String, Integer>> createTeam(@RequestHeader("Authorization") String authorization, @RequestBody TeamAddRequestDto teamAddRequestDto) {
        Map<String, Integer> result = teamService.createTeam(authorization, teamAddRequestDto);
        return ResponseEntity.ok(result);
    }

    // 팀 삭제
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Map<String, String>> deleteTeam(@RequestHeader("Authorization") String authorization, @PathVariable("teamId") int teamId) {
        return ResponseEntity.ok(teamService.deleteTeam(authorization, teamId));
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<Map<String, String>> editTeam(@RequestHeader("Authorization") String authorization, @PathVariable("teamId") int teamId, @RequestBody TeamAddRequestDto teamAddRequestDto) {
        return ResponseEntity.ok(teamService.editTeam(authorization, teamId, teamAddRequestDto));
    }

}
