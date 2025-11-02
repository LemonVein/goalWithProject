package com.jason.goalwithproject;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class GoalWithProjectApplication {

    @PostConstruct
    public void started() {
        // 애플리케이션의 기본 시간대를 한국 서울로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(GoalWithProjectApplication.class, args);
    }

}
