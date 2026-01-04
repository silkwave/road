package com.example.road.data;

import com.example.road.common.LoadBalancedItem;
import lombok.Data;
import lombok.AllArgsConstructor;

// Lombok의 @Data 어노테이션은 보일러플레이트 코드(getter, setter, equals, hashCode, toString)를 자동으로 생성해줍니다.
@Data
@AllArgsConstructor
public class ServerInstance implements LoadBalancedItem {
    // 서버 인스턴스의 고유 식별자
    private Long id;
    // 서버의 이름
    private String name;
    // 서버의 접근 URL
    private String url;
    // 서버의 활성화 상태 (true: 활성, false: 비활성)
    private boolean active;
}