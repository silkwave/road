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
R.O.A.D. (Round-robin Optimized Access Director) 프로젝트는 게이트웨이가 트래픽을 안내하는 '길'임을 강조하며, 동적으로 변화하는 서버 목록을 주기적으로 헬스체크하고 라운드 로빈 방식으로 트래픽을 분산하는 시스템입니다. 핵심 로직은 `RoundRobinLoadBalancer`와 `ServerLoadBalancer`에 구현되어 있으며, 내부적으로 `BlockingQueue`를 사용하여 순환 무결성을 보장합니다.

## 2. 기술 스택
- **언어**: Java 21
- **프레임워크**: Spring Boot 3.2.1
- **데이터베이스**: H2 Database (인메모리)
- **ORM**: MyBatis (MyBatis Spring Boot Starter 3.0.3)
- **빌드 도구**: Gradle
- **웹**: Spring Boot Starter Web, Spring Boot Starter WebFlux (WebClient 사용)
- **유틸리티**: Lombok
- **개발 도구**: Spring Boot DevTools, `jakarta.annotation.api`, `springboot-starter-validation`
- **테스트**: Spring Boot Starter Test, MyBatis Spring Boot Starter Test, MockWebServer
- **로깅**: Logback, `net.logstash.logback:logstash-logback-encoder` (JSON 로깅)

## 3. 핵심 컴포넌트

### 3.1. `ServerInstance.java` (com.example.road.data)
로드 밸런싱될 서버의 정보를 담는 데이터 클래스입니다.
- `id`: 서버 고유 ID
- `name`: 서버 이름
- `url`: 서버 접속 URL
- `active`: 서버의 활성 상태 (DB에 저장된 상태)

```java
// ServerInstance.java
@Data // Lombok을 사용하여 Getter, Setter, equals, hashCode, toString 자동 생성
public class ServerInstance {
    private Long id;
    private String name;
    private String url;
    private boolean active;
}
```

### 3.2. `ServerMapper.java` (com.example.road.mapper)
MyBatis를 사용하여 `servers` 테이블에 접근하는 매퍼 인터페이스입니다.
- `findAllServers()`: 모든 서버 인스턴스 목록을 조회합니다.
- `findById(Long id)`: 특정 ID의 서버 인스턴스를 조회합니다.
- `insertServer(ServerInstance server)`: 새로운 서버를 추가합니다.
- `updateServer(ServerInstance server)`: 기존 서버 정보를 업데이트합니다.
- `deleteServer(Long id)`: 특정 ID의 서버를 삭제합니다.

```java
// ServerMapper.java (MyBatis)
@Mapper // MyBatis 매퍼임을 나타냅니다.
public interface ServerMapper {
    List<ServerInstance> findAllServers();
    Optional<ServerInstance> findById(Long id);
    void insertServer(ServerInstance server);
    void updateServer(ServerInstance server);
    void deleteServer(Long id);
}
```

### 3.3. `RoundRobinLoadBalancer.java` (com.example.road.common)
제네릭 타입 `T`를 사용하여 라운드 로빈 방식으로 아이템을 분배하는 핵심 로직을 담고 있습니다.
- `BlockingQueue`를 내부적으로 사용하여 스레드 안전하게 순환 무결성을 보장합니다.
- `refreshItems()`: 새로운 아이템 목록으로 밸런서를 업데이트합니다. 이때 기존 큐의 내용이 새 아이템 목록으로 대체됩니다.
- `next()`: 큐에서 다음 아이템을 가져옵니다. 사용 가능한 아이템이 없으면 설정된 타임아웃까지 대기합니다.

```java
// RoundRobinLoadBalancer.java
public class RoundRobinLoadBalancer<T> {
    private final BlockingQueue<T> queue;
    private final Predicate<T> activePredicate;
    private final Function<T, ?> idExtractor;
    private final long timeoutSeconds; // poll 대기 시간

    // ... (생성자 및 메서드 구현)

    // 사용 예시
    // 생성 시 활성 판별자 및 ID 추출기를 전달
    RoundRobinLoadBalancer<ServerInstance> balancer = new RoundRobinLoadBalancer<>(
            "ServerInstances", // 로드 밸런서 이름 (로깅용)
            5,                 // poll 대기 타임아웃 (초)
            ServerInstance::isActive, // 활성 여부 판단 Predicate
            ServerInstance::getId     // ID 추출 Function
    );

    // 헬스체크 후
    balancer.refreshItems(healthyServers);
    ServerInstance next = balancer.next().orElse(null);
}
```

### 3.4. `ServerLoadBalancer.java` (com.example.road.service)
실제 서버 인스턴스에 대한 로드 밸런싱 및 헬스체크 로직을 구현한 서비스입니다.
- `@PostConstruct` `init()`: 서비스 초기화 시 `RoundRobinLoadBalancer`를 생성하고 초기 서버 목록을 로드합니다.
- `@Scheduled` `refreshServers()`: 설정된 주기(`server.healthcheck.interval-ms`)마다 DB에서 모든 서버 목록을 가져와 병렬로 헬스체크를 수행합니다. 건강한 서버들로 `RoundRobinLoadBalancer`를 새로고침합니다.
- `@Async` `isServerHealthy()`: 비동기적으로 각 서버의 헬스 상태를 확인합니다. `WebClient`를 사용하여 HEAD 요청을 보내 응답 코드를 확인합니다.
- `getNextServer()`: `RoundRobinLoadBalancer`를 통해 다음 사용 가능한 서버를 반환합니다.
- `getAllServerHealthStatuses()`: 현재 추적 중인 모든 서버의 실시간 헬스 상태(`ServerHealthStatus`) 목록을 반환합니다.
- `@EventListener` `handleServerListChanged()`: `ServerListChangedEvent`가 발생하면 `refreshServers()`를 호출하여 서버 목록을 즉시 새로고침합니다.

### 3.5. `LoadBalancerController.java` (com.example.road.controller)
클라이언트의 부하 분산 요청을 처리하는 REST 컨트롤러입니다.
- `GET /api/dispatch`: `ServerLoadBalancer`를 통해 다음 서버를 가져와 클라이언트에게 반환합니다. 사용 가능한 서버가 없으면 `503 Service Unavailable`을 반환합니다.

### 3.6. `ServerAdminController.java` (com.example.road.controller)
서버 인스턴스를 관리하는 REST 컨트롤러입니다.
- `GET /api/admin/servers`: 모든 서버 목록을 조회합니다.
- `GET /api/admin/servers/{id}`: 특정 서버를 조회합니다.
- `POST /api/admin/servers`: 새로운 서버를 추가합니다.
- `PUT /api/admin/servers/{id}`: 기존 서버 정보를 업데이트합니다.
- `DELETE /api/admin/servers/{id}`: 특정 서버를 삭제합니다.
- `GET /api/admin/servers/health`: 모든 서버의 실시간 헬스 상태를 조회합니다.

### 3.7. `index.html` (src/main/resources/static)
프론트엔드 대시보드 페이지입니다.
- 서버 추가, 활성/비활성 토글, 삭제 기능을 제공합니다.
- `/api/dispatch` 엔드포인트를 호출하여 부하 분산 테스트를 수행합니다.
- 주기적으로 `/api/admin/servers/health`를 호출하여 서버들의 실시간 헬스 상태를 대시보드에 표시합니다.
- 바닐라 JavaScript를 사용하여 비동기 통신 및 DOM 조작을 처리합니다.

## 4. 환경 설정 (`application.yml` 및 `application-dev.yml`)

### 4.1. 데이터베이스 설정
- H2 인메모리 데이터베이스 사용: `jdbc:h2:mem:road_db`
- `spring.sql.init.mode=always`: 애플리케이션 시작 시 `schema.sql` 및 `data.sql` 스크립트 실행
- `mybatis.mapper-locations`: MyBatis 매퍼 XML 파일 위치 지정 (`classpath:/mapper/*.xml`)
- `mybatis.configuration.map-underscore-to-camel-case`: DB 컬럼명-Java 필드명 자동 매핑

### 4.2. 헬스체크 및 비동기 설정
- `server.healthcheck.interval-ms`: 헬스 체크 주기 (기본 10초)
- `server.healthcheck.connection-timeout-ms`, `server.healthcheck.read-timeout-ms`: `WebClient`의 연결/읽기 타임아웃 (기본 3초)
- 비동기 헬스체크를 위한 `HealthCheckConfig.healthCheckExecutor()` (corePoolSize=5, maxPoolSize=10, queueCapacity=25) 설정

### 4.3. 라운드 로빈 구성
- `roundrobin.loadbalancer.timeout-seconds`: `RoundRobinLoadBalancer`의 `next()` 메서드 대기 타임아웃 (기본 5초)

### 4.4. 개발 도구 및 로깅
- `application-dev.yml`에서 H2 콘솔 활성화 (`spring.h2.console.enabled=true`)
- `application-dev.yml`에서 `devtools` 라이브 리로드 활성화 (`spring.devtools.livereload.enabled=true`)
- `logback-spring.xml`을 통해 `dev` 프로파일 시 `com.example.road` 패키지의 로그 레벨을 `DEBUG`로 설정 가능.
- 콘솔 로그와 JSON 파일 로그 (`logs/application.json`) 동시 기록.

## 5. 보안
- 현재 프로젝트는 개발 편의를 위해 Spring Security 자동 구성을 비활성화했습니다 (`spring.autoconfigure.exclude`).
- 운영 환경에서는 적절한 인증/인가 구성 (예: JWT, OAuth2) 및 HTTPS 설정이 필수적입니다.

## 6. 빠른 시작

### 6.1. 로컬 실행 (개발 프로파일)
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```
- H2 콘솔 및 DevTools가 활성화됩니다.

### 6.2. JAR 파일 실행
```bash
./gradlew bootJar
java -jar build/libs/road-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 6.3. H2 콘솔 접근
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:road_db`
- User Name: `sa`
- Password: (비워둡니다)

### 6.4. 대시보드 접근
- URL: `http://localhost:8080/index.html`

## 7. 테스트
- `SpringBootTest`와 `MockMvc`를 사용하여 컨트롤러 및 서비스 계층을 테스트합니다.
- `JdbcTemplate`를 활용하여 통합 테스트 시 DB 초기화 후 롤백 처리를 수행합니다.
- `spring-security-test` 의존성이 제거되어 보안 비활성화 상태에 맞춰 테스트가 작성되었습니다.