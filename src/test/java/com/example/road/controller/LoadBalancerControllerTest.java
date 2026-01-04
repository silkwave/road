package com.example.road.controller;

import com.example.road.data.ServerInstance;
import com.example.road.service.ServerLoadBalancer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// WebMvcTest는 웹 계층(컨트롤러) 테스트에 필요한 빈만 로드합니다.
@WebMvcTest(LoadBalancerController.class)
class LoadBalancerControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청을 시뮬레이션하는 데 사용됩니다.

    @MockBean
    private ServerLoadBalancer serverLoadBalancer; // ServerLoadBalancer는 MockBean으로 주입됩니다.

    @Autowired
    private ObjectMapper objectMapper; // JSON 직렬화/역직렬화를 위한 유틸리티

    @Test
    @DisplayName("dispatchRequest 엔드포인트가 서버 인스턴스를 올바르게 반환하는지 테스트")
    void dispatchRequestReturnsServerInstance() throws Exception {
        // given: ServerLoadBalancer가 특정 ServerInstance를 반환하도록 Mocking합니다.
        ServerInstance mockServer = new ServerInstance(1L, "Test Server", "http://test.com", true);
        when(serverLoadBalancer.getNextServer()).thenReturn(mockServer);

        // when & then: GET 요청을 "/api/dispatch"로 보내고 응답을 검증합니다.
        mockMvc.perform(get("/api/dispatch"))
                .andExpect(status().isOk()) // HTTP 상태 코드가 200 OK인지 확인
                .andExpect(content().json(objectMapper.writeValueAsString(mockServer))); // 반환된 JSON이 예상과 일치하는지 확인
    }

    @Test
    @DisplayName("ServerLoadBalancer에서 예외 발생 시 컨트롤러가 예외를 처리하는지 테스트")
    void dispatchRequestHandlesServiceException() throws Exception {
        // given: ServerLoadBalancer.getNextServer() 호출 시 예외를 발생시키도록 Mocking합니다.
        when(serverLoadBalancer.getNextServer()).thenThrow(new InterruptedException("Service Interrupted"));

        // when & then: GET 요청을 "/api/dispatch"로 보내고 예외 처리(500 Internal Server Error)를 확인합니다.
        mockMvc.perform(get("/api/dispatch"))
                .andExpect(status().isInternalServerError()); // HTTP 상태 코드가 500 Internal Server Error인지 확인
    }
}