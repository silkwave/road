
GEMINI.md

## 사용자 선호사항
- **언어**: 코드 내 주석과 Gemini의 답변은 **한글**로 작성합니다.
- **jdk21**
- **BlockingQueue take put**
- **h2**
- **mybatis**
- **index.html**
- **application.yml**
- **바닐라js**
- **devtool**
- **gradle**
- **롬복**

# 프로젝트 분석: Round Robin Load Balancer

## 1. 개요
R.O.A.D.	Round-robin Optimized Access Director	게이트웨이가 트래픽을 안내하는 '길'임을 강조

이 프로젝트는 동적으로 변화하는 서버 목록을 주기적으로 헬스체크하고, 라운드 로빈 방식으로 트래픽을 분산합니다. 핵심 로직은 `RoundRobinLoadBalancer`와 `ServerLoadBalancer`에 구현되어 있으며, 내부적으로 `BlockingQueue`를 사용해 순환 무결성을 보장합니다.



dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    runtimeOnly 'com.h2database:h2'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

// ServerInstance.java
@Data
public class ServerInstance {
    private Long id;
    private String name;
    private String url;
    private boolean active;
}

// ServerMapper.java (MyBatis)
@Mapper
public interface ServerMapper {
    @Select("SELECT * FROM servers WHERE active = true")
    List<ServerInstance> findActiveServers();
}


`ServerLoadBalancer`는 실제 구현체입니다. 주요 특징:

- DB에서 서버 목록을 로드하고 비동기 헬스체크를 통해 건강한 서버만 필터링합니다.
- `RoundRobinLoadBalancer<ServerInstance>`를 내부에서 사용하여 보관된 서버를 순환합니다.

예시 (간단화된 사용 예):

```java
// 생성 시 활성 판별자 및 ID 추출기를 전달
RoundRobinLoadBalancer<ServerInstance> balancer = new RoundRobinLoadBalancer<>(
        "ServerInstances",
        5,
        ServerInstance::isActive,
        ServerInstance::getId
);

// 헬스체크 후
balancer.refreshItems(healthyServers);
ServerInstance next = balancer.next().orElse(null);
```

서비스는 `@Scheduled`와 `@Async`를 활용하여 주기적이고 병렬화된 헬스체크를 수행합니다.

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>R.O.A.D. Dashboard</title>
</head>
<body>
    <h1>Round Robin Traffic Director</h1>
    <button onclick="dispatchRequest()">요청 보내기</button>
    <div id="log"></div>

    <script>
        async function dispatchRequest() {
            const response = await fetch('/api/dispatch');
            const data = await response.json();
            const logDiv = document.getElementById('log');
            logDiv.innerHTML += `<p>요청이 전송됨 -> <b>${data.url}</b> (${data.name})</p>`;
        }
    </script>
</body>
</html>

spring:
  datasource:
    url: jdbc:h2:mem:road_db
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: (application.yml 기본은 false, 개발 환경에서 활성화 가능)
  devtools:
    livereload:
      enabled: true

mybatis:
  configuration:
    map-underscore-to-camel-case: true


요약 및 특징

- **순환 무결성**: `RoundRobinLoadBalancer` 내부의 `BlockingQueue`(`take()`/`put()`)로 순환 무결성 보장

- **확장성**: 비동기 헬스체크와 병렬화로 여러 요청/스레드 환경에서도 안정적 운영 가능

- **최신 스택**: JDK 21, Spring Boot, MyBatis, H2 등을 사용하여 개발이 용이함

- **보안 주의**: 현재 프로젝트는 개발 편의를 위해 Spring Security 자동 구성을 비활성화(`spring.autoconfigure.exclude`)했고, `SecurityConfig`는 제거되었습니다. 운영 환경에서는 별도 인증/인가 구성 적용이 필수입니다.

**빠른 시작 요약**: 로컬 실행은 `./gradlew bootRun --args='--spring.profiles.active=dev'` 또는 `./gradlew bootJar && java -jar ... --spring.profiles.active=dev`, H2 콘솔은 `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:road_db`, user: `sa`)를 사용하세요.

---

## 개발환경 분석 (요약)
아래 내용은 개발 생산성에 영향을 주는 주요 설정과 권장 작업입니다.

### 프로파일 및 개발 도구
- 개발 전용 프로파일: `src/main/resources/application-dev.yml` (H2 콘솔 및 devtools 활성화). 기본 `application.yml`은 H2 콘솔을 비활성화합니다.
- Spring Boot DevTools 의존성: `build.gradle`의 `developmentOnly libs.springboot.devtools` — 코드 변경 시 자동 리스타트/라이브 리로드가 가능합니다.

### 로그 및 디버깅
- `src/main/resources/logback-spring.xml`에서 `dev` 프로파일 시 `com.example.road` 로거를 DEBUG로 설정하여 개발 시 상세 로그 확인 가능.
- 개발 시 콘솔 로그와 JSON 파일 로그(프로덕션용)를 동시에 기록하도록 구성되어 있습니다 (`logs/application.json`).

### 헬스체크 및 비동기 설정
- 헬스체크 스케줄: `server.healthcheck.interval-ms` (기본 10000 ms) — `ServerLoadBalancer#refreshServers()`가 주기 실행됩니다.
- 비동기 헬스체크 Executor: `HealthCheckConfig.healthCheckExecutor()`
  - corePoolSize=5, maxPoolSize=10, queueCapacity=25, threadNamePrefix="HealthCheck-" (필요 시 조정 권장)
- RestTemplate 시간초과: `server.healthcheck.connection-timeout-ms` 및 `server.healthcheck.read-timeout-ms` (기본 5000 ms)

### 라운드 로빈 구성
- `roundrobin.loadbalancer.timeout-seconds` (기본 5초): `RoundRobinLoadBalancer`의 `poll` 대기 시간에 해당합니다.
- `RoundRobinLoadBalancer`는 생성자에 활성 판별자(Predicate)와 ID 추출기(Function)를 받아 유연하게 동작합니다.

### 데이터베이스 및 테스트
- H2 인메모리 DB: `jdbc:h2:mem:road_db` (개발 시 빠른 초기화와 테스트에 편리)
- 테스트는 `SpringBootTest` + `MockMvc`를 사용하며, 통합 테스트에서 `JdbcTemplate`로 DB 초기화 후 롤백 처리를 합니다.
- `spring-security-test`는 제거되어 테스트는 보안 비활성화 상태에 맞게 정리되었습니다.

### 권장 개발 워크플로우 및 체크리스트
- 로컬 실행(개발): `./gradlew bootRun --args='--spring.profiles.active=dev'` (H2 콘솔 및 devtools 활성화)
- 빠른 이슈 분석: `logs/application.json`과 콘솔 로그를 함께 확인
- 프로덕션 배포 전 체크: 보안(인증/인가) 활성화, HTTPS 설정, 시크릿/환경변수 관리, 로깅 정책 검토
- 퍼포먼스 조정: 헬스체크 Executor 및 RestTemplate 타임아웃을 트래픽 특성에 맞게 튜닝

---

필요하시면 이 섹션을 `README.md`로 동기화하거나, 운영 환경(production)용 체크리스트를 상세화해 드리겠습니다.
