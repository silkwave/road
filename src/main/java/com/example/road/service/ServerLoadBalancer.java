package com.example.road.service;

import com.example.road.common.RoundRobinLoadBalancer;
import com.example.road.data.ServerHealthStatus;
import com.example.road.data.ServerInstance;
import com.example.road.mapper.ServerMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture; // 추가
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async; // 추가

/**
 * 서버 인스턴스에 대한 로드 밸런싱을 담당하는 서비스입니다.
 * 내부적으로 RoundRobinLoadBalancer를 사용하여 로드 밸런싱 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServerLoadBalancer {

    private final ServerMapper serverMapper;
    private final WebClient webClient;
    // application.yml에서 타임아웃 설정을 주입받습니다.
    @Value("${roundrobin.loadbalancer.timeout-seconds:5}") // Default to 5 seconds if not set
    private long loadBalancerTimeoutSeconds;

    @Value("${server.healthcheck.interval-ms:30000}") // Default to 30 seconds
    private long healthCheckIntervalMs;

    private RoundRobinLoadBalancer<ServerInstance> balancer;

    // 모든 서버 인스턴스의 헬스 상태를 추적하는 맵
    // key: serverId, value: ServerHealthStatus
    private final Map<Long, ServerHealthStatus> serverHealthStatuses = new ConcurrentHashMap<>();

    /**
     * 서비스 초기화 시 RoundRobinLoadBalancer를 생성하고 서버 목록을 로드합니다.
     */
    @PostConstruct
    public void init() {
        log.info("서버 로드 밸런서 초기화를 시작합니다.");
        // 주입받은 타임아웃 설정을 사용하여 RoundRobinLoadBalancer를 생성합니다.
        this.balancer = new RoundRobinLoadBalancer<>("ServerInstances", loadBalancerTimeoutSeconds, ServerInstance::isActive, ServerInstance::getId);
        refreshServers();
        log.info("서버 로드 밸런서 초기화를 완료했습니다.");
    }

    /**
     * 데이터베이스에서 서버 목록을 다시 로드하여 로드 밸런서를 새로고침합니다.
     * fixedRate로 주기적으로 실행되며, ServerListChangedEvent에 의해서도 실행됩니다.
     */
    @Scheduled(fixedRateString = "${server.healthcheck.interval-ms:30000}") // 30초마다 헬스 체크
    public void refreshServers() {
        log.info("서버 목록 새로고침 및 헬스 체크를 시작합니다.");
        List<ServerInstance> allServers = serverMapper.findAllServers(); // 모든 서버를 가져옴

        // 헬스 체크를 병렬로 실행하고 결과를 수집합니다.
        List<CompletableFuture<ServerInstance>> healthCheckFutures = allServers.stream()
                .map(server -> this.isServerHealthy(server).thenApply(isHealthy -> { // this.isServerHealthy() 호출
                    serverHealthStatuses.put(server.getId(), new ServerHealthStatus(server, isHealthy, System.currentTimeMillis()));
                    return isHealthy ? server : null;
                }))
                .collect(Collectors.toList());

        // 모든 헬스 체크가 완료될 때까지 기다리고 건강한 서버만 필터링합니다.
        List<ServerInstance> healthyServersForBalancer = healthCheckFutures.stream()
                .map(CompletableFuture::join) // 결과가 나올 때까지 대기 (예외 발생 시 전파)
                .filter(java.util.Objects::nonNull) // null이 아닌 (즉, 건강한) 서버만 필터링
                .collect(Collectors.toList());

        balancer.refreshItems(healthyServersForBalancer);
        log.info("서버 목록 새로고침 및 헬스 체크 완료. 로드 밸런서의 활성 서버 수: {}. 현재 큐의 아이템 수: {}", healthyServersForBalancer.size(), balancer.getActiveItemCount());
    }

    /**
     * 주어진 서버 인스턴스가 정상 상태인지 확인합니다.
     *
     * @param server 확인할 서버 인스턴스
     * @return 서버가 정상이면 true, 그렇지 않으면 false
     */
    @Async("healthCheckExecutor") // HealthCheckConfig에서 정의한 Executor 사용
    public CompletableFuture<Boolean> isServerHealthy(ServerInstance server) {
        if (!server.isActive()) {
            return CompletableFuture.completedFuture(false);
        }
        if (!validateUrl(server.getUrl())) {
            log.warn("서버 {}의 URL {}이 유효하지 않습니다.", server.getName(), server.getUrl());
            return CompletableFuture.completedFuture(false);
        }

        return webClient.head()
                .uri(server.getUrl())
                .retrieve()
                .toBodilessEntity()
                .map(response -> {
                    boolean isHealthy = response.getStatusCode().is2xxSuccessful();
                    if (isHealthy) {
                        log.debug("서버 {} ({}) 헬스 체크 성공.", server.getName(), server.getUrl());
                    } else {
                        log.warn("서버 {} ({}) 헬스 체크 실패: 상태 코드 {}", server.getName(), server.getUrl(), response.getStatusCode());
                    }
                    return isHealthy;
                })
                .toFuture()
                .exceptionally(ex -> {
                    log.warn("서버 {} ({}) 헬스 체크 중 오류 발생: {}", server.getName(), server.getUrl(), ex.getMessage());
                    return false;
                });
    }

    /**
     * 주어진 URL 문자열이 유효한 형식인지 검증합니다.
     *
     * @param urlString 검증할 URL 문자열
     * @return 유효한 URL이면 true, 그렇지 않으면 false
     */
    private boolean validateUrl(String urlString) {
        try {
            URI.create(urlString).toURL();
            return true;
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }

    /**
     * 라운드 로빈 방식으로 다음 서버 인스턴스를 가져옵니다.
     *
     * @return 다음 서버 인스턴스를 포함하는 Optional. 사용 가능한 서버가 없으면 빈 Optional 반환.
     * @throws InterruptedException 스레드가 대기 중에 인터럽트될 경우 발생
     */
    public Optional<ServerInstance> getNextServer() throws InterruptedException {
        return balancer.next();
    }

    /**
     * 현재 모든 서버의 헬스 상태를 반환합니다.
     * @return 모든 서버의 헬스 상태 목록
     */
    public List<ServerHealthStatus> getAllServerHealthStatuses() {
        return List.copyOf(serverHealthStatuses.values());
    }

    /**
     * 헬스 상태 맵을 지웁니다. 주로 테스트용으로 사용됩니다.
     */
    public void clearServerHealthStatuses() {
        this.serverHealthStatuses.clear();
    }


}
