package com.example.road.controller;

import com.example.road.data.ServerHealthStatus;
import com.example.road.data.ServerInstance;
import com.example.road.service.ServerAdminService;
import com.example.road.service.ServerLoadBalancer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 서버 인스턴스 관리를 위한 REST 컨트롤러입니다.
 * 서버 목록 조회, 추가, 수정, 삭제 및 헬스 상태 조회 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/admin/servers")
@RequiredArgsConstructor
public class ServerAdminController {

    private final ServerAdminService serverAdminService;
    private final ServerLoadBalancer serverLoadBalancer; // ServerLoadBalancer 주입

    /**
     * 모든 서버 인스턴스 목록을 조회합니다.
     * @return 모든 서버 인스턴스 목록을 담은 ResponseEntity
     */
    @GetMapping
    public ResponseEntity<List<ServerInstance>> getAllServers() {
        return ResponseEntity.ok(serverAdminService.getAllServers());
    }

    /**
     * 현재 추적 중인 모든 서버의 실시간 헬스 상태를 반환합니다.
     * @return 모든 서버의 헬스 상태 목록을 담은 ResponseEntity
     */
    @GetMapping("/health")
    public ResponseEntity<List<ServerHealthStatus>> getAllServerHealth() {
        return ResponseEntity.ok(serverLoadBalancer.getAllServerHealthStatuses());
    }

    /**
     * 새로운 서버 인스턴스를 추가합니다.
     * @param server 추가할 서버 인스턴스 정보 (요청 본문)
     * @return 추가된 서버 인스턴스 정보를 담은 ResponseEntity
     */
    @PostMapping
    public ResponseEntity<ServerInstance> addServer(@Valid @RequestBody ServerInstance server) {
        ServerInstance newServer = serverAdminService.addServer(server);
        return ResponseEntity.ok(newServer);
    }

    /**
     * 특정 ID의 서버 인스턴스 정보를 업데이트합니다.
     * @param id 업데이트할 서버의 ID (경로 변수)
     * @param server 업데이트할 서버 인스턴스 정보 (요청 본문)
     * @return 업데이트된 서버 인스턴스 정보를 담은 ResponseEntity
     */
    @PutMapping("/{id}")
    public ResponseEntity<ServerInstance> updateServer(@PathVariable Long id, @Valid @RequestBody ServerInstance server) {
        server.setId(id); // URL 경로의 ID를 서버 객체에 설정
        ServerInstance updatedServer = serverAdminService.updateServer(server);
        return ResponseEntity.ok(updatedServer);
    }

    /**
     * 특정 ID의 서버 인스턴스를 삭제합니다.
     * @param id 삭제할 서버의 ID (경로 변수)
     * @return 성공 시 204 No Content 응답
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverAdminService.deleteServer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 서버 목록을 수동으로 새로고침하고 헬스 체크를 강제 실행하는 엔드포인트.
     * 이 엔드포인트를 호출하면 ServerLoadBalancer가 데이터베이스에서 서버 목록을 다시 로드하고 헬스 체크를 수행합니다.
     * @return 성공 시 200 OK 응답
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshServers() {
        serverLoadBalancer.refreshServers();
        return ResponseEntity.ok().build();
    }
}