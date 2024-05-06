package com.example.clubservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/*
the important point here is both monolithId and serviceId space are mutually exclusive. In other words, any monolithId
cannot exist in serviceId space, or vice versa. This is a very important to uniquely map any entity across the monolith and the service
 */
@Entity
@Table(name = "id_mappings")
public class IdMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long serviceId;
    private Long monolithId;
    private String typeName;

    public IdMapping() {
    }

    public IdMapping(Long serviceId, Long monolithId, String typeName) {
        this.serviceId = serviceId;
        this.monolithId = monolithId;
        this.typeName = typeName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getMonolithId() {
        return monolithId;
    }

    public void setMonolithId(Long monolithId) {
        this.monolithId = monolithId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdMapping idMapping = (IdMapping) o;
        return Objects.equals(serviceId, idMapping.serviceId) && Objects.equals(monolithId, idMapping.monolithId) && Objects.equals(typeName, idMapping.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, monolithId, typeName);
    }

    @Override
    public String toString() {
        return "IdMapping{" +
                "id=" + id +
                ", serviceId=" + serviceId +
                ", monolithId=" + monolithId +
                ", typeName='" + typeName + '\'' +
                '}';
    }
}
