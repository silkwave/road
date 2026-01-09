package com.example.road.service;

import com.example.road.data.ServerHealthStatus;
import com.example.road.data.ServerInstance;
import com.example.road.mapper.ServerMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.awaitility.Awaitility;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
class ServerLoadBalancerTest {

    @Autowired
    private ServerLoadBalancer serverLoadBalancer;

    @MockBean
    private ServerMapper serverMapper;

    private MockWebServer mockWebServer1;
    private MockWebServer mockWebServer2;
    private MockWebServer mockWebServer3;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer1 = new MockWebServer();
        mockWebServer1.start();
        mockWebServer2 = new MockWebServer();
        mockWebServer2.start();
        mockWebServer3 = new MockWebServer();
        mockWebServer3.start();

        List<ServerInstance> initialServers = Arrays.asList(
                new ServerInstance(1L, "Server A", mockWebServer1.url("/").toString(), true),
                new ServerInstance(2L, "Server B", mockWebServer2.url("/").toString(), true),
                new ServerInstance(3L, "Server C", mockWebServer3.url("/").toString(), true)
        );

        when(serverMapper.findAllServers()).thenReturn(initialServers);
        serverLoadBalancer.clearServerHealthStatuses();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer1.shutdown();
        mockWebServer2.shutdown();
        mockWebServer3.shutdown();
    }

    @Test
    @DisplayName("refreshServers 호출 시 서버 목록이 새로고침되고 헬스 체크 상태가 업데이트되는지 테스트")
    void refreshServersUpdatesTheServerListAndHealthStatus() {
        // given: Server B는 비정상이라고 가정
        mockWebServer1.enqueue(new MockResponse().setResponseCode(200));
        mockWebServer2.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer3.enqueue(new MockResponse().setResponseCode(200));

        // when: refreshServers를 호출합니다.
        serverLoadBalancer.refreshServers();

        // then: Awaitility를 사용하여 healthCheckExecutor의 비동기 작업이 완료될 때까지 기다립니다.
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<ServerHealthStatus> healthStatuses = serverLoadBalancer.getAllServerHealthStatuses();
            assertThat(healthStatuses).hasSize(3);
            assertThat(healthStatuses).anyMatch(s -> s.getServerInstance().getName().equals("Server A") && s.isHealthy());
            assertThat(healthStatuses).anyMatch(s -> s.getServerInstance().getName().equals("Server B") && !s.isHealthy());
            assertThat(healthStatuses).anyMatch(s -> s.getServerInstance().getName().equals("Server C") && s.isHealthy());

            Optional<ServerInstance> s1 = serverLoadBalancer.getNextServer();
            Optional<ServerInstance> s2 = serverLoadBalancer.getNextServer();
            Optional<ServerInstance> s3 = serverLoadBalancer.getNextServer();

            assertThat(s1).isPresent().get().extracting(ServerInstance::getName).isEqualTo("Server A");
            assertThat(s2).isPresent().get().extracting(ServerInstance::getName).isEqualTo("Server C");
            assertThat(s3).isPresent().get().extracting(ServerInstance::getName).isEqualTo("Server A");
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