package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.FileEntity;
import com.lockerroom.resourceservice.model.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType targetType, Long targetId);
}
