package com.example.clubservice.repository;

import com.example.clubservice.model.IdMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdMappingRepository extends JpaRepository<IdMapping, Long> {
    IdMapping findByServiceIdAndTypeName(Long serviceId, String typeName);
    IdMapping findByMonolithIdAndTypeName(Long monolithId, String typeName);
}
