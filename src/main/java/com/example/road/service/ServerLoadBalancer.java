package com.example.road.service;

import com.example.road.common.RoundRobinLoadBalancer;
import com.example.road.data.ServerInstance;
import com.example.road.mapper.ServerMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 서버 인스턴스에 대한 로드 밸런싱을 담당하는 서비스입니다.
 * 내부적으로 RoundRobinLoadBalancer를 사용하여 로드 밸런싱 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServerLoadBalancer {

    private final ServerMapper serverMapper;
    private RoundRobinLoadBalancer<ServerInstance> balancer;

    /**
     * 서비스 초기화 시 RoundRobinLoadBalancer를 생성하고 서버 목록을 로드합니다.
     */
    @PostConstruct
    public void init() {
        log.info("서버 로드 밸런서 초기화를 시작합니다.");
        this.balancer = new RoundRobinLoadBalancer<>("ServerInstances");
        refreshServers();
        log.info("서버 로드 밸런서 초기화를 완료했습니다.");
    }

    /**
     * 데이터베이스에서 서버 목록을 다시 로드하여 로드 밸런서를 새로고침합니다.
     */
    public void refreshServers() {
        log.info("서버 목록 새로고침을 시작합니다.");
        List<ServerInstance> allServers = serverMapper.findActiveServers(); // 현재는 활성 서버만 가져오지만, 전체를 가져와서 필터링하는 구조로 변경 가능
        balancer.refreshItems(allServers);
    }

    /**
     * 라운드 로빈 방식으로 다음 서버 인스턴스를 가져옵니다.
     *
     * @return 다음 서버 인스턴스
     * @throws InterruptedException 스레드가 대기 중에 인터럽트될 경우 발생
     */
    public ServerInstance getNextServer() throws InterruptedException {
        return balancer.next();
    }

    /**
     * ServerListChangedEvent가 발생했을 때 서버 목록을 새로고침하는 이벤트 리스너입니다.
     * @param event 서버 목록 변경 이벤트
     */
    @EventListener
    public void handleServerListChanged(ServerListChangedEvent event) {
        log.info("서버 목록 변경 이벤트를 감지했습니다. 서버 목록을 새로고침합니다.");
        refreshServers();
    }
}
