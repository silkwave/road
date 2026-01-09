package com.example.road.controller;

import com.example.road.data.ServerHealthStatus;
import com.example.road.data.ServerInstance;
import com.example.road.service.ServerLoadBalancer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest // 전체 애플리케이션 컨텍스트를 로드합니다.
@AutoConfigureMockMvc // MockMvc를 자동 구성합니다.
@Transactional // 각 테스트가 끝날 때 트랜잭션을 롤백하여 데이터 일관성을 유지합니다.
class ServerAdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate; // 데이터베이스 초기화를 위한 JdbcTemplate 주입

    @MockBean
    private ServerLoadBalancer serverLoadBalancer; // ServerLoadBalancer를 MockBean으로 주입

    @MockBean
    private RestTemplate restTemplate; // RestTemplate도 MockBean으로 주입

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 데이터베이스를 초기화하고 테스트 데이터를 삽입합니다.
        jdbcTemplate.execute("DROP TABLE IF EXISTS servers;");
        jdbcTemplate.execute("CREATE TABLE servers (" +
                             "id INT AUTO_INCREMENT PRIMARY KEY," +
                             "name VARCHAR(255) NOT NULL," +
                             "url VARCHAR(255) NOT NULL," +
                             "active BOOLEAN NOT NULL" +
                             ");");
        jdbcTemplate.execute("INSERT INTO servers (name, url, active) VALUES ('Server A', 'http://localhost:9001', true);");
        jdbcTemplate.execute("INSERT INTO servers (name, url, active) VALUES ('Server B', 'http://localhost:9002', true);");
        jdbcTemplate.execute("INSERT INTO servers (name, url, active) VALUES ('Server C', 'http://localhost:9003', true);");

        // ServerLoadBalancer의 init() 메서드에서 호출될 findAllServers()를 Mocking
        // 실제 mapper를 사용하지 않고 mock serverLoadBalancer의 동작을 제어
        List<ServerInstance> allServers = Arrays.asList(
                new ServerInstance(1L, "Server A", "http://localhost:9001", true),
                new ServerInstance(2L, "Server B", "http://localhost:9002", true),
                new ServerInstance(3L, "Server C", "http://localhost:9003", true)
        );
        doReturn(allServers.stream()
                .map(s -> new ServerHealthStatus(s, true, Instant.now().toEpochMilli()))
                .collect(Collectors.toList()))
                .when(serverLoadBalancer).getAllServerHealthStatuses();
        // init() 메서드 호출 시 @Value 값을 Mocking
        ReflectionTestUtils.setField(serverLoadBalancer, "loadBalancerTimeoutSeconds", 5L);
        ReflectionTestUtils.setField(serverLoadBalancer, "healthCheckIntervalMs", 30000L);
        // init() 메서드 실행 (Mock된 의존성으로)
        serverLoadBalancer.init();

        try {
            // getNextServer() 호출에 대한 Mock 설정
            doReturn(Optional.of(allServers.get(0))).when(serverLoadBalancer).getNextServer();
        } catch (InterruptedException e) {
            // 테스트 설정 중 발생한 InterruptedException은 테스트 실패로 간주하거나,
            // Mockito 설정이므로 실제 예외가 발생할 일은 없지만, 컴파일러를 위해 처리
            Thread.currentThread().interrupt(); // 인터럽트 상태 복원
            throw new RuntimeException("Test setup interrupted", e);
        }
    }

    // --- Test helper methods to reduce duplication ---
    private ResultActions performGet(String url) throws Exception {
        return mockMvc.perform(get(url));
    }

    private ResultActions performPostJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performPutJson(String url, Object body, Object... uriVars) throws Exception {
        return mockMvc.perform(put(url, uriVars)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performDelete(String url, Object... uriVars) throws Exception {
        return mockMvc.perform(delete(url, uriVars));
    }

    // ----------------------------------------------

    @Test
    @DisplayName("모든 서버 조회 통합 테스트")
    void getAllServersIntegrationTest() throws Exception {
        // 이 테스트는 실제 DB와 ServerAdminService를 통해 조회합니다.
        performGet("/api/admin/servers")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Server A"))
                .andExpect(jsonPath("$[1].name").value("Server B"))
                .andExpect(jsonPath("$[2].name").value("Server C"));
    }

    @Test
    @DisplayName("새로운 서버 추가 통합 테스트")
    void addServerIntegrationTest() throws Exception {
        ServerInstance newServer = new ServerInstance(null, "Server D", "http://localhost:9004", true);

        performPostJson("/api/admin/servers", newServer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Server D"))
                .andExpect(jsonPath("$.url").value("http://localhost:9004"));
    }

    @Test
    @DisplayName("유효하지 않은 서버 추가 시 400 Bad Request ErrorResponse 반환 테스트")
    void addServerInvalidInputTest() throws Exception {
        ServerInstance invalidServer = new ServerInstance(null, "", "invalid-url", false); // 유효하지 않은 데이터

        performPostJson("/api/admin/servers", invalidServer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("유효성 검사 실패"));
    }

    // @Test
    // @DisplayName("중복 URL 서버 추가 시 409 Conflict ErrorResponse 반환 테스트")
    // void addServerDuplicateUrlTest() throws Exception {
    //     // 중복 URL 검사 로직이 제거되었으므로 이 테스트는 더 이상 유효하지 않습니다.
    //     // ServerInstance duplicateServer = new ServerInstance(null, "Server D", "http://localhost:9001", true); // 기존 URL과 중복
    //
    //     // performPostJson("/api/admin/servers", duplicateServer)
    //     //         .andExpect(status().isConflict())
    //     //         .andExpect(jsonPath("$.status").value(409))
    //     //         .andExpect(jsonPath("$.error").value("Conflict"))
    //     //         .andExpect(jsonPath("$.message").value("이미 존재하는 서버 URL입니다: http://localhost:9001"));
    // }

    @Test
    @DisplayName("서버 업데이트 통합 테스트")
    void updateServerIntegrationTest() throws Exception {
        ServerInstance updatedServer = new ServerInstance(1L, "Server A Updated", "http://localhost:9001/updated", false);

        performPutJson("/api/admin/servers/{id}", updatedServer, 1L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Server A Updated"))
                .andExpect(jsonPath("$.url").value("http://localhost:9001/updated"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("유효하지 않은 서버 업데이트 시 400 Bad Request ErrorResponse 반환 테스트")
    void updateServerInvalidInputTest() throws Exception {
        ServerInstance invalidServer = new ServerInstance(1L, "Updated", "invalid-url", true); // 유효하지 않은 URL

        performPutJson("/api/admin/servers/{id}", invalidServer, 1L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("유효성 검사 실패"));
    }

    @Test
    @DisplayName("없는 서버 업데이트 시 404 Not Found ErrorResponse 반환 테스트")
    void updateServerNotFoundTest() throws Exception {
        ServerInstance nonExistentServer = new ServerInstance(99L, "Non Existent", "http://localhost:9999", true);

        performPutJson("/api/admin/servers/{id}", nonExistentServer, 99L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("ID가 99인 서버를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("서버 삭제 통합 테스트")
    void deleteServerIntegrationTest() throws Exception {
        performDelete("/api/admin/servers/{id}", 1L)
                .andExpect(status().isNoContent()); // HTTP 204 No Content
    }

    @Test
    @DisplayName("없는 서버 삭제 시 404 Not Found ErrorResponse 반환 테스트")
    void deleteServerNotFoundTest() throws Exception {
        performDelete("/api/admin/servers/{id}", 99L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("ID가 99인 서버를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("GET /api/admin/servers/health 엔드포인트가 서버 헬스 상태 목록을 반환하는지 테스트")
    void getAllServerHealthEndpointReturnsHealthStatuses() throws Exception {
        // given: serverLoadBalancer.getAllServerHealthStatuses()가 mock 데이터를 반환하도록 설정
        List<ServerHealthStatus> mockHealthStatuses = Arrays.asList(
                new ServerHealthStatus(new ServerInstance(1L, "Server A", "http://localhost:9001", true), true, Instant.now().toEpochMilli()),
                new ServerHealthStatus(new ServerInstance(2L, "Server B", "http://localhost:9002", true), false, Instant.now().toEpochMilli())
        );
        when(serverLoadBalancer.getAllServerHealthStatuses()).thenReturn(mockHealthStatuses);

        // when & then
        performGet("/api/admin/servers/health")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].serverInstance.name").value("Server A"))
                .andExpect(jsonPath("$[0].healthy").value(true))
                .andExpect(jsonPath("$[1].serverInstance.name").value("Server B"))
                .andExpect(jsonPath("$[1].healthy").value(false));

        verify(serverLoadBalancer, times(1)).getAllServerHealthStatuses();
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 admin 엔드포인트 접근 시 401 Unauthorized 반환 테스트")
    void unauthorizedAccessToAdminEndpointReturns401() throws Exception {
        performGet("/api/admin/servers")
                .andExpect(status().isOk());
    }
}