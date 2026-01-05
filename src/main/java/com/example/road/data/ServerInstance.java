package com.example.road.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import java.util.Objects;

// Lombok의 @Getter, @Setter, @AllArgsConstructor 어노테이션은 해당 메서드들을 자동으로 생성해줍니다.
// @NoArgsConstructor를 추가하여 기본 생성자를 명시적으로 제공합니다.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServerInstance {
    // 서버 인스턴스의 고유 식별자
    private Long id;
    // 서버의 이름
    @NotBlank(message = "서버 이름은 비워둘 수 없습니다.")
    private String name;
    // 서버의 접근 URL
    @NotBlank(message = "서버 URL은 비워둘 수 없습니다.")
    @URL(message = "유효한 URL 형식이 아닙니다.")
    private String url;
    // 서버의 활성화 상태 (true: 활성, false: 비활성)
    @NotNull(message = "활성 상태는 필수입니다.")
    private boolean active;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerInstance that = (ServerInstance) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ServerInstance(" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", url='" + url + '\'' +
               ", active=" + active +
               ')';
    }
}