package com.example.road.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient 설정을 위한 구성 클래스입니다.
 * 헬스체크 및 기타 외부 서비스 호출에 사용되는 WebClient 인스턴스를 정의합니다.
 */
@Configuration
public class WebClientConfig {

    // application.yml에서 연결 타임아웃 값을 주입받습니다. 기본값은 5000ms (5초)입니다.
    @Value("${server.healthcheck.connection-timeout-ms:5000}")
    private int connectionTimeout;

    // application.yml에서 읽기 타임아웃 값을 주입받습니다. 기본값은 5000ms (5초)입니다.
    @Value("${server.healthcheck.read-timeout-ms:5000}")
    private int readTimeout;

    /**
     * WebClient 빈을 생성하고 구성합니다.
     * Reactor Netty HttpClient를 사용하여 연결 및 응답 타임아웃을 설정합니다.
     * @return 구성된 WebClient 인스턴스
     */
    @Bean
    public WebClient webClient() {
        // HttpClient 설정: 응답 타임아웃과 연결 타임아웃을 적용합니다.
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(readTimeout)) // 응답을 기다리는 최대 시간
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout); // 연결을 시도하는 최대 시간

        // WebClient.Builder를 사용하여 WebClient 인스턴스를 빌드합니다.
        // 위에서 설정한 HttpClient를 사용하여 클라이언트 커넥터를 생성합니다.
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}