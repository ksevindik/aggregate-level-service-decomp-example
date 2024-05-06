package com.example.clubservice.migration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "service.migration")
public class MigrationProperties {
    private String sourceDbUrl;
    private String targetDbUrl;
    private String sourceDbUsername;
    private String sourceDbPassword;
    private String targetDbUsername;
    private String targetDbPassword;
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

    public String getTargetDbUrl() {
        return targetDbUrl;
    }

    public void setTargetDbUrl(String targetDbUrl) {
        this.targetDbUrl = targetDbUrl;
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

    public String getTargetDbUsername() {
        return targetDbUsername;
    }

    public void setTargetDbUsername(String targetDbUsername) {
        this.targetDbUsername = targetDbUsername;
    }

    public String getTargetDbPassword() {
        return targetDbPassword;
    }

    public void setTargetDbPassword(String targetDbPassword) {
        this.targetDbPassword = targetDbPassword;
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
