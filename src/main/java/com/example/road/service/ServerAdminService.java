package com.example.road.service;

import com.example.road.data.ServerInstance;
import com.example.road.exception.DuplicateServerException;
import com.example.road.exception.ServerNotFoundException;
import com.example.road.mapper.ServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

        // 중복 URL 검사
        Optional<ServerInstance> existingServer = serverMapper.findByUrl(server.getUrl());
        if (existingServer.isPresent()) {
            throw new DuplicateServerException("이미 존재하는 서버 URL입니다: " + server.getUrl());
        }

        serverMapper.insertServer(server);
        eventPublisher.publishEvent(new ServerListChangedEvent(this));
        log.info("서버 추가 후 ServerListChangedEvent를 발행했습니다.");
        return server;
    }

    @Transactional
    public ServerInstance updateServer(ServerInstance server) {
        log.info("서버 정보를 업데이트합니다: {}", server);

        // 업데이트 시 중복 URL 검사 (자신을 제외하고 중복되는지 확인)
        Optional<ServerInstance> existingServer = serverMapper.findByUrl(server.getUrl());
        if (existingServer.isPresent() && !existingServer.get().getId().equals(server.getId())) {
            throw new DuplicateServerException("이미 존재하는 서버 URL입니다: " + server.getUrl());
        }

        int updatedRows = serverMapper.updateServer(server);
        if (updatedRows == 0) {
            throw new ServerNotFoundException("ID가 " + server.getId() + "인 서버를 찾을 수 없습니다.");
        }
        eventPublisher.publishEvent(new ServerListChangedEvent(this));
        log.info("서버 업데이트 후 ServerListChangedEvent를 발행했습니다.");
        return server;
    }

    @Transactional
    public void deleteServer(Long id) {
        log.info("서버를 삭제합니다: ID={}", id);
        int deletedRows = serverMapper.deleteServer(id);
        if (deletedRows == 0) {
            throw new ServerNotFoundException("ID가 " + id + "인 서버를 찾을 수 없습니다.");
        }
        eventPublisher.publishEvent(new ServerListChangedEvent(this));
        log.info("서버 삭제 후 ServerListChangedEvent를 발행했습니다.");
    }
}
