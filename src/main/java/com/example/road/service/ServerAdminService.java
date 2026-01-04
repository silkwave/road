package com.example.road.service;

import com.example.road.data.ServerInstance;
import com.example.road.mapper.ServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServerAdminService {

    private final ServerMapper serverMapper;
    private final ApplicationEventPublisher eventPublisher;

    public List<ServerInstance> getAllServers() {
        log.debug("모든 서버 목록을 조회합니다.");
        return serverMapper.findAllServers();
    }

    @Transactional
    public ServerInstance addServer(ServerInstance server) {
        log.info("새로운 서버를 추가합니다: {}", server);
        serverMapper.insertServer(server);
        eventPublisher.publishEvent(new ServerListChangedEvent(this));
        log.info("서버 추가 후 ServerListChangedEvent를 발행했습니다.");
        return server;
    }

    @Transactional
    public ServerInstance updateServer(ServerInstance server) {
        log.info("서버 정보를 업데이트합니다: {}", server);
        serverMapper.updateServer(server);
        eventPublisher.publishEvent(new ServerListChangedEvent(this));
        log.info("서버 업데이트 후 ServerListChangedEvent를 발행했습니다.");
        return server;
    }

    @Transactional
    public void deleteServer(Long id) {
        log.info("서버를 삭제합니다: ID={}", id);
        serverMapper.deleteServer(id);
        eventPublisher.publishEvent(new ServerListChangedEvent(this));
        log.info("서버 삭제 후 ServerListChangedEvent를 발행했습니다.");
    }
}
