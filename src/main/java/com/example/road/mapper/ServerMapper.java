package com.example.road.mapper;

import com.example.road.data.ServerInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

import java.util.List;
import java.util.Optional; // Optional import 추가

// MyBatis 매퍼 인터페이스임을 나타냅니다.
// 이 인터페이스는 데이터베이스와의 상호작용을 정의합니다.
@Mapper
public interface ServerMapper {
    // 활성 상태인 서버 인스턴스 목록을 데이터베이스에서 조회하는 메서드입니다.
    List<ServerInstance> findActiveServers();

    // 모든 서버 인스턴스 목록을 데이터베이스에서 조회합니다.
    List<ServerInstance> findAllServers();

    // ID를 기준으로 서버 인스턴스를 조회합니다.
    Optional<ServerInstance> findById(Long id);

    // URL을 기준으로 서버 인스턴스를 조회합니다.
    Optional<ServerInstance> findByUrl(String url);

    // 새로운 서버 인스턴스를 데이터베이스에 삽입합니다.
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertServer(ServerInstance server);

    // 기존 서버 인스턴스의 정보를 업데이트합니다.
    int updateServer(ServerInstance server);

    // ID를 기준으로 서버 인스턴스를 삭제합니다.
    int deleteServer(Long id);
}