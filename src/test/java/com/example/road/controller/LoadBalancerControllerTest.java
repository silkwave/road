package com.example.road.controller;

import com.example.road.service.ServerLoadBalancer;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

// WebMvcTest는 웹 계층(컨트롤러) 테스트에 필요한 빈만 로드합니다.
@WebMvcTest(LoadBalancerController.class)
class LoadBalancerControllerTest {

    @MockBean
    private ServerLoadBalancer serverLoadBalancer; // ServerLoadBalancer는 MockBean으로 주입됩니다.

}