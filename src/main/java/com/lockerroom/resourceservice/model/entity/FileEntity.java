package com.lockerroom.resourceservice.model.entity;

import com.lockerroom.resourceservice.model.enums.TargetType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "files", indexes = {
        @Index(name = "idx_files_target", columnList = "target_type, target_id"),
        @Index(name = "idx_files_user", columnList = "user_id")
})
public class FileEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String storedName;

    @Column(nullable = false, length = 500)
    private String s3Path;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false, length = 100)
    private String mimeType;
}
