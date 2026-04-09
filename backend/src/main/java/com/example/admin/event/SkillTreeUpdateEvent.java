package com.example.admin.event;

import org.springframework.context.ApplicationEvent;

public class SkillTreeUpdateEvent extends ApplicationEvent {
    private final Long userId;

    public SkillTreeUpdateEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
}