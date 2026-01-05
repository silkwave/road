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

@RestController
@RequestMapping("/api/admin/servers")
@RequiredArgsConstructor
public class ServerAdminController {

    private final ServerAdminService serverAdminService;
    private final ServerLoadBalancer serverLoadBalancer; // ServerLoadBalancer 주입

    @GetMapping
    public ResponseEntity<List<ServerInstance>> getAllServers() {
        return ResponseEntity.ok(serverAdminService.getAllServers());
    }

    // 모든 서버의 헬스 상태를 반환하는 새로운 엔드포인트
    @GetMapping("/health")
    public ResponseEntity<List<ServerHealthStatus>> getAllServerHealth() {
        return ResponseEntity.ok(serverLoadBalancer.getAllServerHealthStatuses());
    }

    @PostMapping
    public ResponseEntity<ServerInstance> addServer(@Valid @RequestBody ServerInstance server) {
        ServerInstance newServer = serverAdminService.addServer(server);
        return ResponseEntity.ok(newServer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServerInstance> updateServer(@PathVariable Long id, @Valid @RequestBody ServerInstance server) {
        server.setId(id); // URL 경로의 ID를 서버 객체에 설정
        ServerInstance updatedServer = serverAdminService.updateServer(server);
        return ResponseEntity.ok(updatedServer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverAdminService.deleteServer(id);
        return ResponseEntity.noContent().build();
    }
}
