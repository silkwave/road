package com.example.road.service;

import com.example.road.data.ServerHealthStatus;
import com.example.road.data.ServerInstance;
import com.example.road.mapper.ServerMapper;
import org.awaitility.Awaitility; // Awaitility 임포트
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit; // TimeUnit 임포트
import org.springframework.boot.test.context.SpringBootTest; // 추가
import org.springframework.boot.test.mock.mockito.MockBean; // 추가
import org.springframework.http.HttpHeaders; // 추가
import org.springframework.boot.test.mock.mockito.SpyBean; // 추가

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest // Spring Boot 테스트 컨텍스트 로드
class ServerLoadBalancerTest {

    @MockBean // Spring Context에 Mock으로 등록
    private ServerMapper serverMapper;

    @MockBean // Spring Context에 Mock으로 등록
    private RestTemplate restTemplate; // RestTemplate Mock 객체로 변경

    @SpyBean // Real bean이지만 스파이로 동작하여 특정 메서드를 mocking 가능
    private ServerLoadBalancer serverLoadBalancer;

    private List<ServerInstance> initialServers;

    @BeforeEach
    void setUp() throws InterruptedException {
        initialServers = Arrays.asList(
                new ServerInstance(1L, "Server A", "http://server-a.com", true),
                new ServerInstance(2L, "Server B", "http://server-b.com", true),
                new ServerInstance(3L, "Server C", "http://server-c.com", true)
        );
        when(serverMapper.findAllServers()).thenReturn(initialServers);

        // @Value 필드 수동 주입 (Mocking이 안 되므로)
        ReflectionTestUtils.setField(serverLoadBalancer, "loadBalancerTimeoutSeconds", 5L);
        ReflectionTestUtils.setField(serverLoadBalancer, "healthCheckIntervalMs", 30000L);

        // 테스트를 위해 serverHealthStatuses 맵을 초기화 (이전 테스트의 상태가 남아있을 수 있으므로)
        serverLoadBalancer.clearServerHealthStatuses();

        // Mock init() so it doesn't run during @PostConstruct
        doNothing().when(serverLoadBalancer).init();

        // 모든 서버가 정상이라고 가정하고 RestTemplate.headForHeaders 호출을 Mocking
        when(restTemplate.headForHeaders(anyString())).thenReturn(new HttpHeaders()); // Corrected stubbing for non-void method

        // All mocks are set up, now manually call init() to trigger logic.
        serverLoadBalancer.init();
    }


    @Test
    @DisplayName("refreshServers 호출 시 서버 목록이 새로고침되고 헬스 체크 상태가 업데이트되는지 테스트")
    void refreshServersUpdatesTheServerListAndHealthStatus() throws InterruptedException {
        // given: Server B는 비정상이라고 가정
        doThrow(new ResourceAccessException("Connection refused")).when(restTemplate).headForHeaders("http://server-b.com");

        // when: refreshServers를 호출합니다.
        serverLoadBalancer.refreshServers();

        // then: Awaitility를 사용하여 healthCheckExecutor의 비동기 작업이 완료될 때까지 기다립니다.
        // healthCheckExecutor가 동기적으로 실행되도록 Mocking했으므로, 이 시점에는 이미 완료된 상태입니다.
        // 하지만 실제 비동기 환경을 시뮬레이션하는 경우 Awaitility가 필요합니다.
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // 로드 밸런서에는 Server A와 Server C만 존재해야 합니다.
            Optional<ServerInstance> s1 = serverLoadBalancer.getNextServer();
            Optional<ServerInstance> s2 = serverLoadBalancer.getNextServer();
            Optional<ServerInstance> s3 = serverLoadBalancer.getNextServer();

            assertThat(s1).isPresent();
            assertThat(s2).isPresent();
            assertThat(s3).isPresent();

            assertThat(s1.get().getName()).isEqualTo("Server A");
            assertThat(s2.get().getName()).isEqualTo("Server C");
            assertThat(s3.get().getName()).isEqualTo("Server A");

            // getAllServerHealthStatuses를 통해 헬스 상태가 정확히 반영되었는지 확인
            List<ServerHealthStatus> healthStatuses = serverLoadBalancer.getAllServerHealthStatuses();
            assertThat(healthStatuses).hasSize(3);
            assertThat(healthStatuses).anyMatch(s -> s.getServerInstance().getName().equals("Server A") && s.isHealthy());
            assertThat(healthStatuses).anyMatch(s -> s.getServerInstance().getName().equals("Server B") && !s.isHealthy());
            assertThat(healthStatuses).anyMatch(s -> s.getServerInstance().getName().equals("Server C") && s.isHealthy());
        });
    }



    @Test
    @DisplayName("활성 서버가 없는 경우 빈 Optional을 반환하는지 테스트")
    void getNextServerReturnsEmptyOptionalWhenNoActiveServers() throws InterruptedException {
        // given: 모든 서버가 비정상이거나 없도록 Mocking
        when(serverMapper.findAllServers()).thenReturn(Collections.emptyList());
        serverLoadBalancer.refreshServers(); // 서버 목록을 비웁니다.

        // when: 다음 서버를 요청합니다.
        Optional<ServerInstance> server = serverLoadBalancer.getNextServer();

        // then: 빈 Optional이 반환되는지 확인합니다.
        assertThat(server).isEmpty();
    }








}
