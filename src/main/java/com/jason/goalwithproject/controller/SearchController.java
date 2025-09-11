package com.jason.goalwithproject.controller;

import com.jason.goalwithproject.domain.user.User;
import com.jason.goalwithproject.dto.peer.RequesterDto;
import com.jason.goalwithproject.dto.team.TeamResponseDto;
import com.jason.goalwithproject.dto.user.UserDto;
import com.jason.goalwithproject.service.TeamService;
import com.jason.goalwithproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final UserService userService;
    private final TeamService teamService;

    @GetMapping("/user")
    public ResponseEntity<Page<RequesterDto>> getSearchUsers(@RequestHeader("Authorization") String authorization, @RequestParam("search") String keyword, Pageable pageable) {
        Page<RequesterDto> pages = userService.searchUsers(authorization, keyword, pageable);
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/team")
    public ResponseEntity<Page<TeamResponseDto>> getSearchUsers(@RequestParam("search") String keyword, Pageable pageable) {
        Page<TeamResponseDto> pages = teamService.searchTeams(keyword, pageable);
        return ResponseEntity.ok(pages);
    }
}
