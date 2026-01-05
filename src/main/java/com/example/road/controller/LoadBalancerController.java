package com.example.road.controller;

import com.example.road.data.ServerInstance;
import com.example.road.service.ServerLoadBalancer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

// 이 클래스가 RESTful 웹 서비스의 컨트롤러임을 나타냅니다.
@RestController
// 이 컨트롤러의 모든 핸들러 메서드는 "/api" 경로로 시작하는 요청을 처리합니다.
@RequestMapping("/api")
// Lombok 어노테이션으로, final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 합니다.
@RequiredArgsConstructor
@Slf4j
public class LoadBalancerController {

    // 로드 밸런싱 로직을 제공하는 서비스를 주입받습니다.
    private final ServerLoadBalancer serverLoadBalancer;

    // HTTP GET 요청이 "/api/dispatch" 경로로 들어올 때 이 메서드가 호출됩니다.
    @GetMapping("/dispatch")
    public ResponseEntity<ServerInstance> dispatchRequest() throws InterruptedException {
        log.info("라우트 요청을 받았습니다.");
        // ServerLoadBalancer 통해 라운드 로빈 방식으로 다음 서버 인스턴스를 가져옵니다.
        Optional<ServerInstance> serverOptional = serverLoadBalancer.getNextServer();

        if (serverOptional.isEmpty()) {
            log.warn("사용 가능한 서버가 없습니다. HTTP 503 Service Unavailable 응답을 반환합니다.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        ServerInstance server = serverOptional.get();
        log.info("다음 서버로 라우팅합니다: {}", server);
        // 가져온 서버 인스턴스 정보를 HTTP 200 OK 응답과 함께 반환합니다.
        return ResponseEntity.ok(server);
    }
}