package com.example.clubservice.migration;

import org.springframework.context.ApplicationEvent;

public class EntityPersistedEvent extends ApplicationEvent {
    private Object entity;
    public EntityPersistedEvent(Object source, Object entity) {
        super(source);
        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }
}
