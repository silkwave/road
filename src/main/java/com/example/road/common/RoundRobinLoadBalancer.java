package com.example.road.common;

import lombok.extern.slf4j.Slf4j;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 제네릭 라운드 로빈 로드 밸런서 구현체입니다.
 * 로드 밸런싱 대상 객체에 대해 로드 밸런싱을 수행할 수 있습니다.
 * 이 클래스는 스레드에 안전합니다.
 *
 * @param <T> 로드 밸런싱 대상 객체의 타입
 */
@Slf4j
public class RoundRobinLoadBalancer<T> {

    private final BlockingQueue<T> itemQueue = new LinkedBlockingQueue<>();
    private final String name;
    private final long timeoutSeconds;
    private final Predicate<T> activePredicate;
    private final Function<T, Long> idFunction;

    /**
     * 지정된 이름으로 라운드 로빈 로드 밸런서를 생성합니다.
     * @param name 로드 밸런서의 이름 (로깅에 사용)
     */
    public RoundRobinLoadBalancer(String name) {
        this(name, 5, t -> true, t -> null); // Default: accept all items, id extractor returns null
    }

    /**
     * 지정된 이름과 타임아웃으로 라운드 로빈 로드 밸런서를 생성합니다.
     * @param name 로드 밸런서의 이름 (로깅에 사용)
     * @param timeoutSeconds 다음 아이템을 기다릴 최대 시간 (초)
     */
    public RoundRobinLoadBalancer(String name, long timeoutSeconds) {
        this(name, timeoutSeconds, t -> true, t -> null);
    }

    /**
     * 지정된 이름, 타임아웃, 활성 판별자 및 ID 추출기를 사용하여 라운드 로빈 로드 밸런서를 생성합니다.
     */
    public RoundRobinLoadBalancer(String name, long timeoutSeconds, java.util.function.Predicate<T> activePredicate, java.util.function.Function<T, Long> idFunction) {
        this.name = name;
        this.timeoutSeconds = timeoutSeconds;
        this.activePredicate = activePredicate;
        this.idFunction = idFunction;
        log.info("[{}] 라운드 로빈 로드 밸런서가 생성되었습니다. 타임아웃: {}초", name, timeoutSeconds);
    }

    /**
     * 로드 밸런서의 아이템 목록을 새로고침합니다.
     * 활성 상태인 아이템만 큐에 추가됩니다.
     *
     * @param allItems 전체 아이템 목록
     */
    public void refreshItems(java.util.List<T> allItems) { // 여기는 이미 수정됨
        log.info("[{}] 아이템 목록 새로고침을 시작합니다...", name);
        itemQueue.clear();
        java.util.List<T> activeItems = allItems.stream() // 여기를 수정
                .filter(activePredicate)
                .collect(Collectors.toList());
        itemQueue.addAll(activeItems);
        log.info("[{}] {}개의 활성 아이템을 로드했습니다. (전체: {}개)", name, activeItems.size(), allItems.size());
    }

    /**
     * 라운드 로빈 방식으로 다음 아이템을 가져옵니다.
     * 큐에서 아이템을 하나 꺼내고, 즉시 다시 큐의 끝에 추가하여 순환 구조를 유지합니다.
     * 큐가 비어있으면, 지정된 시간 동안 아이템이 추가될 때까지 대기합니다.
     *
     * @return 다음 아이템을 포함하는 Optional. 아이템을 가져올 수 없으면 빈 Optional 반환.
     * @throws InterruptedException 스레드가 대기 중에 인터럽트될 경우 발생
     */
    public Optional<T> next() throws InterruptedException {
        log.debug("[{}] 다음 아이템을 요청합니다. (타임아웃: {}초)", name, timeoutSeconds);
        T item = itemQueue.poll(timeoutSeconds, TimeUnit.SECONDS);

        if (item == null) {
            log.warn("[{}] {}초 동안 다음 아이템을 가져오지 못했습니다. 큐가 비어 있거나 사용 가능한 아이템이 없습니다.", name, timeoutSeconds);
            return Optional.empty();
        }

        Long id = idFunction != null ? idFunction.apply(item) : null;
        log.debug("[{}] 아이템 '{}'를 선택했습니다.", name, id);

        try {
            return Optional.of(item);
        } finally {
            itemQueue.put(item);
            log.debug("[{}] 아이템 '{}'를 큐에 다시 추가했습니다.", name, id);
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
