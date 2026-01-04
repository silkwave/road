package com.example.road.mapper;

import com.example.road.data.ServerInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// MyBatis 관련 컴포넌트만 스캔하여 테스트 컨텍스트를 로드합니다.
@MybatisTest
// 실제 데이터베이스가 아닌 내장된 테스트 데이터베이스를 사용하도록 설정합니다.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ServerMapperTest {

    @Autowired
    private ServerMapper serverMapper; // 테스트 대상 매퍼 주입

    @Test
    @DisplayName("활성 서버 목록 조회 테스트")
    void findActiveServersTest() {
        // given: 테스트를 위한 초기 데이터는 src/main/resources/data.sql에 정의되어 있습니다.
        // Server A, B, C가 활성 상태로 가정

        // when: 활성 서버 목록을 조회합니다.
        List<ServerInstance> activeServers = serverMapper.findActiveServers();

        // then: 조회된 서버 목록이 예상과 일치하는지 확인합니다.
        assertThat(activeServers).isNotNull(); // 목록이 null이 아닌지 확인
        assertThat(activeServers).hasSize(3);  // 3개의 활성 서버가 있는지 확인
        assertThat(activeServers)
                .extracting(ServerInstance::getName)
                .containsExactlyInAnyOrder("Server A", "Server B", "Server C"); // 서버 이름 확인
    }
}