package com.example.clubservice.migration;

import org.springframework.context.ApplicationEvent;

/*
entity persisted events are published by the entity persister to notify the listeners about the entity persist
process is completed on the service side. the main use case of those events currently are to make tests to wait for the
entity change events to be processed by the entity persister on the service side.
 */
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
