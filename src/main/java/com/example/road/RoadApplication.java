package com.example.road;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// 이 어노테이션은 Spring Boot 애플리케이션의 시작점임을 나타냅니다.
// 자동 구성을 활성화하고, 컴포넌트 스캔을 수행하며, Spring Boot 설정을 활성화합니다.
@SpringBootApplication
@EnableScheduling // 스케줄링 활성화
public class RoadApplication {

    // 애플리케이션의 메인 메서드입니다.
    // 여기서 Spring Boot 애플리케이션이 시작됩니다.
    public static void main(String[] args) {
        SpringApplication.run(RoadApplication.class, args);
    }

}