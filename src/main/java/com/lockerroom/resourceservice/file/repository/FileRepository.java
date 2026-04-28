package com.lockerroom.resourceservice.file.repository;

import com.lockerroom.resourceservice.file.model.entity.FileEntity;
import com.lockerroom.resourceservice.file.model.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType targetType, Long targetId);

    List<FileEntity> findAllByIdInAndDeletedAtIsNull(List<Long> ids);
}
