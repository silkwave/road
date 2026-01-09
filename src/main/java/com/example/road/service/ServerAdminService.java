package com.example.road.service;

import com.example.road.data.ServerInstance;
import com.example.road.exception.DuplicateServerException;
import com.example.road.exception.ServerNotFoundException;
import com.example.road.mapper.ServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import jakarta.annotation.PostConstruct;

import java.util.List;

/**
 * 서버 인스턴스 관리와 관련된 비즈니스 로직을 처리하는 서비스입니다.
 * 서버 추가, 조회, 수정, 삭제 기능을 제공하며, 변경 사항 발생 시 로드 밸런서 새로고침을 트리거합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServerAdminService {

    private final ServerMapper serverMapper;
    private final WebClient webClient; // WebClient 주입
    private String refreshEndpointUrl; // 로드 밸런서 새로고침을 위한 내부 API 엔드포인트 URL

    /**
     * 서비스 초기화 시 로드 밸런서 새로고침 엔드포인트 URL을 설정합니다.
     * (현재 로컬 개발 환경을 가정하고 하드코딩되어 있으며, 운영 환경에서는 외부 설정으로 관리하는 것이 좋습니다.)
     */
    @PostConstruct
    public void init() {
        this.refreshEndpointUrl = "http://localhost:8080/api/admin/servers/refresh";
        log.info("서버 새로고침 엔드포인트 URL 설정됨: {}", refreshEndpointUrl);
    }

    /**
     * 모든 서버 인스턴스 목록을 조회합니다.
     * @return 모든 서버 인스턴스 목록
     */
    public List<ServerInstance> getAllServers() {
        log.debug("모든 서버 목록을 조회합니다.");
        return serverMapper.findAllServers();
    }

    /**
     * 새로운 서버 인스턴스를 추가합니다.
     * 서버 URL 중복을 검사하고, 추가 성공 시 로드 밸런서 새로고침을 트리거합니다.
     * @param server 추가할 서버 인스턴스
     * @return 추가된 서버 인스턴스
     * @throws DuplicateServerException 이미 존재하는 서버 URL인 경우
     */
    @Transactional
    public ServerInstance addServer(ServerInstance server) {
        log.info("새로운 서버를 추가합니다: {}", server);



        serverMapper.insertServer(server);
        refreshServers(); // 서버 변경 후 새로고침 엔드포인트 호출
        log.info("서버 추가 후 새로고침 엔드포인트를 호출했습니다.");
        return server;
    }

    /**
     * 기존 서버 인스턴스 정보를 업데이트합니다.
     * 서버 URL 중복을 검사하고, 업데이트 성공 시 로드 밸런서 새로고침을 트리거합니다.
     * @param server 업데이트할 서버 인스턴스
     * @return 업데이트된 서버 인스턴스
     * @throws DuplicateServerException 이미 존재하는 서버 URL인 경우 (자신을 제외)
     * @throws ServerNotFoundException 해당 ID의 서버를 찾을 수 없는 경우
     */
    @Transactional
    public ServerInstance updateServer(ServerInstance server) {
        log.info("서버 정보를 업데이트합니다: {}", server);



        int updatedRows = serverMapper.updateServer(server);
        if (updatedRows == 0) {
            throw new ServerNotFoundException("ID가 " + server.getId() + "인 서버를 찾을 수 없습니다.");
        }
        refreshServers(); // 서버 변경 후 새로고침 엔드포인트 호출
        log.info("서버 업데이트 후 새로고침 엔드포인트를 호출했습니다.");
        return server;
    }

    /**
     * 특정 ID의 서버 인스턴스를 삭제합니다.
     * 삭제 성공 시 로드 밸런서 새로고침을 트리거합니다.
     * @param id 삭제할 서버의 ID
     * @throws ServerNotFoundException 해당 ID의 서버를 찾을 수 없는 경우
     */
    @Transactional
    public void deleteServer(Long id) {
        log.info("서버를 삭제합니다: ID={}", id);
        int deletedRows = serverMapper.deleteServer(id);
        if (deletedRows == 0) {
            throw new ServerNotFoundException("ID가 " + id + "인 서버를 찾을 수 없습니다.");
        }
        refreshServers(); // 서버 변경 후 새로고침 엔드포인트 호출
        log.info("서버 삭제 후 새로고침 엔드포인트를 호출했습니다.");
    }

    /**
     * 로드 밸런서의 서버 목록을 새로고침하기 위해 내부 API 엔드포인트를 호출합니다.
     * WebClient를 사용하여 비동기적으로 POST 요청을 보냅니다.
     */
    private void refreshServers() {
        webClient.post()
                .uri(refreshEndpointUrl)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> log.debug("로드 밸런서 새로고침 엔드포인트 호출 성공: {}", response.getStatusCode()),
                        error -> log.error("로드 밸런서 새로고침 엔드포인트 호출 실패: {}", error.getMessage())
                );
    }
}