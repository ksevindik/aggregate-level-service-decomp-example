package com.example.clubservice.dynamo.migration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "service.migration")
public class MigrationProperties {
    private String sourceDbUrl;
    private String sourceDbUsername;
    private String sourceDbPassword;
    private String monolithBaseUrl;
    private String entityChangeTopic;
    private String sourceOrigin;
    private String targetOrigin;

    public String getSourceDbUrl() {
        return sourceDbUrl;
    }

    public void setSourceDbUrl(String sourceDbUrl) {
        this.sourceDbUrl = sourceDbUrl;
    }

    public String getSourceDbUsername() {
        return sourceDbUsername;
    }

    public void setSourceDbUsername(String sourceDbUsername) {
        this.sourceDbUsername = sourceDbUsername;
    }

    public String getSourceDbPassword() {
        return sourceDbPassword;
    }

    public void setSourceDbPassword(String sourceDbPassword) {
        this.sourceDbPassword = sourceDbPassword;
    }

    public String getMonolithBaseUrl() {
        return monolithBaseUrl;
    }

    public void setMonolithBaseUrl(String monolithBaseUrl) {
        this.monolithBaseUrl = monolithBaseUrl;
    }

    public String getEntityChangeTopic() {
        return entityChangeTopic;
    }

    public void setEntityChangeTopic(String entityChangeTopic) {
        this.entityChangeTopic = entityChangeTopic;
    }

    public String getSourceOrigin() {
        return sourceOrigin;
    }

    public void setSourceOrigin(String sourceOrigin) {
        this.sourceOrigin = sourceOrigin;
    }

    public String getTargetOrigin() {
        return targetOrigin;
    }

    public void setTargetOrigin(String targetOrigin) {
        this.targetOrigin = targetOrigin;
    }
}
