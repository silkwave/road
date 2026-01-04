package com.example.road.common;

import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * 제네릭 라운드 로빈 로드 밸런서 구현체입니다.
 * LoadBalancedItem 인터페이스를 구현하는 모든 유형의 객체에 대해 로드 밸런싱을 수행할 수 있습니다.
 * 이 클래스는 스레드에 안전합니다.
 *
 * @param <T> 로드 밸런싱 대상 객체의 타입 (LoadBalancedItem을 구현해야 함)
 */
@Slf4j
public class RoundRobinLoadBalancer<T extends LoadBalancedItem> {

    private final BlockingQueue<T> itemQueue = new LinkedBlockingQueue<>();
    private final String name;

    /**
     * 지정된 이름으로 라운드 로빈 로드 밸런서를 생성합니다.
     * @param name 로드 밸런서의 이름 (로깅에 사용)
     */
    public RoundRobinLoadBalancer(String name) {
        this.name = name;
        log.info("[{}] 라운드 로빈 로드 밸런서가 생성되었습니다.", name);
    }

    /**
     * 로드 밸런서의 아이템 목록을 새로고침합니다.
     * 활성 상태인 아이템만 큐에 추가됩니다.
     *
     * @param allItems 전체 아이템 목록
     */
    public void refreshItems(List<T> allItems) {
        log.info("[{}] 아이템 목록 새로고침을 시작합니다...", name);
        itemQueue.clear();
        List<T> activeItems = allItems.stream()
                .filter(LoadBalancedItem::isActive)
                .collect(Collectors.toList());
        itemQueue.addAll(activeItems);
        log.info("[{}] {}개의 활성 아이템을 로드했습니다. (전체: {}개)", name, activeItems.size(), allItems.size());
    }

    /**
     * 라운드 로빈 방식으로 다음 아이템을 가져옵니다.
     * 큐에서 아이템을 하나 꺼내고, 즉시 다시 큐의 끝에 추가하여 순환 구조를 유지합니다.
     * 큐가 비어있으면, 아이템이 추가될 때까지 대기합니다.
     *
     * @return 다음 아이템
     * @throws InterruptedException 스레드가 대기 중에 인터럽트될 경우 발생
     */
    public T next() throws InterruptedException {
        log.debug("[{}] 다음 아이템을 요청합니다.", name);
        // 큐의 헤드에서 아이템을 가져옵니다 (블로킹).
        T item = itemQueue.take();
        try {
            log.debug("[{}] 아이템 '{}'를 선택했습니다.", name, item.getId());
            return item;
        } finally {
            // 아이템을 다시 큐의 끝에 추가합니다.
            itemQueue.put(item);
            log.debug("[{}] 아이템 '{}'를 큐에 다시 추가했습니다.", name, item.getId());
        }
    }

    /**
     * 현재 큐에 있는 아이템의 개수를 반환합니다.
     * @return 활성 아이템의 개수
     */
    public int getActiveItemCount() {
        return itemQueue.size();
    }
}
