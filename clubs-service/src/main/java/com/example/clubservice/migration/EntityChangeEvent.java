package com.example.clubservice.migration;

public class EntityChangeEvent {
    private String action;
    private String type;
    private String origin;
    private String entity;

    public EntityChangeEvent(String action, String type, String origin, String entity) {
        this.type = type;
        this.origin = origin;
        this.action = action;
        this.entity = entity;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public String getEntity() {
        return entity;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }
}
