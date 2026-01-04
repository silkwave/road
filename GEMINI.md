
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

이 프로젝트는 동적으로 변하는 활성 서버 목록을 관리하고, 라운드 로빈(Round Robin) 알고리즘을 사용하여 서버 부하를 분산하는 생산자-소비자(Producer-Consumer) 패턴의 구현체입니다.



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


@Service
@RequiredArgsConstructor
public class LoadBalancerService {
    private final ServerMapper serverMapper;
    // 서버 목록을 순환시키기 위한 BlockingQueue
    private final BlockingQueue<ServerInstance> serverQueue = new LinkedBlockingQueue<>();

    // 애플리케이션 시작 시 DB에서 서버 목록을 불러와 큐를 채움 (Producer 역할)
    @PostConstruct
    public void init() {
        refreshServers();
    }

    public void refreshServers() {
        serverQueue.clear();
        List<ServerInstance> servers = serverMapper.findActiveServers();
        serverQueue.addAll(servers);
    }

    // 라운드 로빈 방식으로 다음 서버를 선택 (Consumer 역할)
    public ServerInstance getNextServer() throws InterruptedException {
        // 큐의 맨 앞에서 꺼냄 (Blocking)
        ServerInstance server = serverQueue.take(); 
        try {
            return server;
        } finally {
            // 사용 후 다시 큐의 맨 뒤로 이동시켜 순환 구조 형성
            serverQueue.put(server);
        }
    }
}

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
      enabled: true
  devtools:
    livereload:
      enabled: true

mybatis:
  configuration:
    map-underscore-to-camel-case: true


요약 및 특징

    순환 무결성: take()와 put()을 사용하여 동기화 이슈 없이 정확하게 모든 서버를 차례대로 순회합니다.

    확장성: BlockingQueue 덕분에 여러 스레드에서 동시에 요청이 들어와도 안전하게 서버를 할당할 수 있습니다.

    최신 스택: JDK 21의 기능을 활용할 준비가 되어 있으며, MyBatis로 유연한 DB 쿼리가 가능합니다.
