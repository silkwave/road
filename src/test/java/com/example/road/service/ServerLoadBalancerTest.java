package com.example.road.service;

import com.example.road.data.ServerInstance;
import com.example.road.mapper.ServerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Mockito를 사용하여 Mock 객체를 주입하고 테스트를 확장합니다.
@ExtendWith(MockitoExtension.class)
class ServerLoadBalancerTest {

    @Mock
    private ServerMapper serverMapper; // ServerMapper는 Mock 객체로 주입됩니다.

    @InjectMocks
    private ServerLoadBalancer serverLoadBalancer; // Mock 객체가 주입될 서비스

    // 각 테스트 실행 전에 초기화 작업을 수행합니다.
    @BeforeEach
    void setUp() {
        // Mock ServerMapper가 findActiveServers() 호출 시 반환할 가상의 서버 목록을 정의합니다.
        List<ServerInstance> mockServers = Arrays.asList(
                new ServerInstance(1L, "Server A", "http://server-a.com", true),
                new ServerInstance(2L, "Server B", "http://server-b.com", true),
                new ServerInstance(3L, "Server C", "http://server-c.com", true)
        );
        // findActiveServers() 메서드가 호출되면 mockServers를 반환하도록 설정합니다.
        when(serverMapper.findActiveServers()).thenReturn(mockServers);

        // 서비스 초기화를 위해 init()을 수동으로 호출합니다.
        serverLoadBalancer.init();
    }

    @Test
    @DisplayName("라운드 로빈 방식으로 서버가 순환되는지 테스트")
    void getNextServerReturnsServersInRoundRobinOrder() throws InterruptedException {
        // when & then: 여러 번 호출하여 라운드 로빈 순서를 확인합니다.
        assertThat(serverLoadBalancer.getNextServer().getName()).isEqualTo("Server A");
        assertThat(serverLoadBalancer.getNextServer().getName()).isEqualTo("Server B");
        assertThat(serverLoadBalancer.getNextServer().getName()).isEqualTo("Server C");
        assertThat(serverLoadBalancer.getNextServer().getName()).isEqualTo("Server A"); // 다시 Server A로 돌아옴
    }

    @Test
    @DisplayName("refreshServers 호출 시 서버 목록이 새로고침되는지 테스트")
    void refreshServersUpdatesTheServerList() throws InterruptedException {
        // given: 현재 큐 상태를 확인합니다.
        assertThat(serverLoadBalancer.getNextServer().getName()).isEqualTo("Server A");

        // when: 새로운 서버 목록을 Mocking하고 refreshServers를 호출합니다.
        List<ServerInstance> newMockServers = Arrays.asList(
                new ServerInstance(4L, "Server D", "http://server-d.com", true),
                new ServerInstance(5L, "Server E", "http://server-e.com", true)
        );
        when(serverMapper.findActiveServers()).thenReturn(newMockServers);
        serverLoadBalancer.refreshServers();

        // then: 새로운 서버 목록으로 큐가 업데이트되었는지 확인합니다.
        assertThat(serverLoadBalancer.getNextServer().getName()).isEqualTo("Server D");
        assertThat(serverLoadBalancer.getNextServer().getName()).isEqualTo("Server E");
        assertThat(serverLoadBalancer.getNextServer().getName()).isEqualTo("Server D");
    }

    @Test
    @DisplayName("서버 인스턴스에 올바른 필드가 포함되어 있는지 테스트")
    void serverInstanceContainsCorrectFields() throws InterruptedException {
        ServerInstance server = serverLoadBalancer.getNextServer();
        assertThat(server.getId()).isEqualTo(1L);
        assertThat(server.getName()).isEqualTo("Server A");
        assertThat(server.getUrl()).isEqualTo("http://server-a.com");
        assertThat(server.isActive()).isTrue();
    }
}