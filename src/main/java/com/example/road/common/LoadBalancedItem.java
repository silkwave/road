package com.example.road.common;

/**
 * 로드 밸런싱 대상이 되는 항목을 위한 인터페이스입니다.
 * 모든 로드 밸런싱 대상은 고유 ID와 활성 상태를 가져야 합니다.
 */
public interface LoadBalancedItem {
    /**
     * 항목의 고유 식별자를 반환합니다.
     * @return 항목의 ID
     */
    Long getId();

    /**
     * 항목이 현재 활성 상태인지 여부를 반환합니다.
     * @return 활성 상태이면 true, 그렇지 않으면 false
     */
    boolean isActive();
}
