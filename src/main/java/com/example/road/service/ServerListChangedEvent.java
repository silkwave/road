package com.example.road.service;

import org.springframework.context.ApplicationEvent;

/**
 * 서버 목록이 변경되었음을 알리는 이벤트 클래스입니다.
 * 이 이벤트는 서버가 추가, 수정 또는 삭제될 때 게시됩니다.
 */
public class ServerListChangedEvent extends ApplicationEvent {

    /**
     * 새로운 ApplicationEvent를 생성합니다.
     * @param source 이벤트를 게시한 소스 객체
     */
    public ServerListChangedEvent(Object source) {
        super(source);
    }
}
