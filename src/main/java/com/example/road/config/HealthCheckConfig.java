package com.example.road.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 헬스 체크를 위한 스레드 풀 Executor를 설정하는 구성 클래스입니다.
 * {@code @EnableAsync}를 통해 애플리케이션 내의 비동기 메서드 실행을 활성화합니다.
 */
@Configuration
@EnableAsync // 비동기 메서드 사용을 활성화합니다.
public class HealthCheckConfig {

    @Bean(name = "healthCheckExecutor")
    public Executor healthCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본 스레드 풀 사이즈
        executor.setMaxPoolSize(10); // 최대 스레드 풀 사이즈
        executor.setQueueCapacity(25); // 큐 용량
        executor.setThreadNamePrefix("HealthCheck-"); // 스레드 이름 접두사
        executor.initialize();
        return executor;
    }
}
