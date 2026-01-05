package com.example.road.mapper;

import com.example.road.data.ServerInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// MyBatis 관련 컴포넌트만 스캔하여 테스트 컨텍스트를 로드합니다.
@MybatisTest
// 실제 데이터베이스가 아닌 내장된 테스트 데이터베이스를 사용하도록 설정합니다.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ServerMapperTest {

    @Autowired
    private ServerMapper serverMapper; // 테스트 대상 매퍼 주입

    @Autowired
    private JdbcTemplate jdbcTemplate; // 데이터베이스 초기화를 위한 JdbcTemplate 주입

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
        jdbcTemplate.execute("INSERT INTO servers (name, url, active) VALUES ('Server D (Inactive)', 'http://localhost:9004', false);");
    }

    @Test
    @DisplayName("활성 서버 목록 조회 테스트")
    void findActiveServersTest() {
        // when: 활성 서버 목록을 조회합니다.
        List<ServerInstance> activeServers = serverMapper.findActiveServers();

        // then: 조회된 서버 목록이 예상과 일치하는지 확인합니다.
        assertThat(activeServers).isNotNull();
        assertThat(activeServers).hasSize(3);
        assertThat(activeServers)
                .extracting(ServerInstance::getName)
                .containsExactlyInAnyOrder("Server A", "Server B", "Server C");
    }

    @Test
    @DisplayName("모든 서버 목록 조회 테스트")
    void findAllServersTest() {
        // when: 모든 서버 목록을 조회합니다.
        List<ServerInstance> allServers = serverMapper.findAllServers();

        // then: 조회된 서버 목록이 예상과 일치하는지 확인합니다.
        assertThat(allServers).isNotNull();
        assertThat(allServers).hasSize(4); // 활성 3개 + 비활성 1개
        assertThat(allServers)
                .extracting(ServerInstance::getName)
                .containsExactlyInAnyOrder("Server A", "Server B", "Server C", "Server D (Inactive)");
    }

    @Test
    @DisplayName("ID로 서버 조회 테스트 - 찾음")
    void findByIdFoundTest() {
        // given: ID가 1인 서버 인스턴스 (Server A)
        Long serverId = 1L;

        // when: ID로 서버를 조회합니다.
        Optional<ServerInstance> foundServer = serverMapper.findById(serverId);

        // then: 서버가 존재하고 정보가 일치하는지 확인합니다.
        assertThat(foundServer).isPresent();
        assertThat(foundServer.get().getName()).isEqualTo("Server A");
        assertThat(foundServer.get().getUrl()).isEqualTo("http://localhost:9001");
    }

    @Test
    @DisplayName("ID로 서버 조회 테스트 - 찾을 수 없음")
    void findByIdNotFoundTest() {
        // given: 존재하지 않는 ID
        Long serverId = 99L;

        // when: ID로 서버를 조회합니다.
        Optional<ServerInstance> foundServer = serverMapper.findById(serverId);

        // then: 서버가 존재하지 않는지 확인합니다.
        assertThat(foundServer).isEmpty();
    }

    @Test
    @DisplayName("URL로 서버 조회 테스트 - 찾음")
    void findByUrlFoundTest() {
        // given: URL이 'http://localhost:9001'인 서버 인스턴스 (Server A)
        String serverUrl = "http://localhost:9001";

        // when: URL로 서버를 조회합니다.
        Optional<ServerInstance> foundServer = serverMapper.findByUrl(serverUrl);

        // then: 서버가 존재하고 정보가 일치하는지 확인합니다.
        assertThat(foundServer).isPresent();
        assertThat(foundServer.get().getName()).isEqualTo("Server A");
        assertThat(foundServer.get().getUrl()).isEqualTo("http://localhost:9001");
    }

    @Test
    @DisplayName("URL로 서버 조회 테스트 - 찾을 수 없음")
    void findByUrlNotFoundTest() {
        // given: 존재하지 않는 URL
        String serverUrl = "http://localhost:9999";

        // when: URL로 서버를 조회합니다.
        Optional<ServerInstance> foundServer = serverMapper.findByUrl(serverUrl);

        // then: 서버가 존재하지 않는지 확인합니다.
        assertThat(foundServer).isEmpty();
    }

    @Test
    @DisplayName("서버 추가 테스트")
    void insertServerTest() {
        // given: 새로운 서버 인스턴스
        ServerInstance newServer = new ServerInstance(null, "Server E", "http://localhost:9005", true);

        // when: 서버를 추가합니다.
        int affectedRows = serverMapper.insertServer(newServer);

        // then: 1개의 행이 영향을 받았고, 서버가 성공적으로 추가되었는지 확인합니다.
        assertThat(affectedRows).isEqualTo(1);
        assertThat(newServer.getId()).isNotNull(); // ID가 자동 생성되었는지 확인

        // 추가된 서버를 조회하여 확인합니다.
        Optional<ServerInstance> foundServer = serverMapper.findById(newServer.getId());
        assertThat(foundServer).isPresent();
        assertThat(foundServer.get().getName()).isEqualTo("Server E");
    }

    @Test
    @DisplayName("서버 업데이트 테스트 - 성공")
    void updateServerSuccessTest() {
        // given: 기존 서버 (ID=1, Server A)의 정보를 업데이트
        ServerInstance existingServer = new ServerInstance(1L, "Server A Updated", "http://localhost:9001/updated", false);

        // when: 서버를 업데이트합니다.
        int affectedRows = serverMapper.updateServer(existingServer);

        // then: 1개의 행이 영향을 받았고, 서버 정보가 업데이트되었는지 확인합니다.
        assertThat(affectedRows).isEqualTo(1);
        Optional<ServerInstance> updatedServer = serverMapper.findById(1L);
        assertThat(updatedServer).isPresent();
        assertThat(updatedServer.get().getName()).isEqualTo("Server A Updated");
        assertThat(updatedServer.get().getUrl()).isEqualTo("http://localhost:9001/updated");
        assertThat(updatedServer.get().isActive()).isFalse();
    }

    @Test
    @DisplayName("서버 업데이트 테스트 - 찾을 수 없음")
    void updateServerNotFoundTest() {
        // given: 존재하지 않는 서버 (ID=99)의 정보를 업데이트 시도
        ServerInstance nonExistentServer = new ServerInstance(99L, "Non Existent", "http://localhost:9999", true);

        // when: 서버를 업데이트합니다.
        int affectedRows = serverMapper.updateServer(nonExistentServer);

        // then: 영향을 받은 행이 없어야 합니다.
        assertThat(affectedRows).isEqualTo(0);
    }

    @Test
    @DisplayName("서버 삭제 테스트 - 성공")
    void deleteServerSuccessTest() {
        // given: ID가 1인 서버 (Server A)를 삭제
        Long serverId = 1L;

        // when: 서버를 삭제합니다.
        int affectedRows = serverMapper.deleteServer(serverId);

        // then: 1개의 행이 영향을 받았고, 서버가 삭제되었는지 확인합니다.
        assertThat(affectedRows).isEqualTo(1);
        assertThat(serverMapper.findById(serverId)).isEmpty();
    }

    @Test
    @DisplayName("서버 삭제 테스트 - 찾을 수 없음")
    void deleteServerNotFoundTest() {
        // given: 존재하지 않는 서버 (ID=99)를 삭제 시도
        Long serverId = 99L;

        // when: 서버를 삭제합니다.
        int affectedRows = serverMapper.deleteServer(serverId);

        // then: 영향을 받은 행이 없어야 합니다.
        assertThat(affectedRows).isEqualTo(0);
    }
}