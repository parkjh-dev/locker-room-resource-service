package com.lockerroom.resourceservice.model.entity;

import com.lockerroom.resourceservice.model.enums.RequestStatus;
import com.lockerroom.resourceservice.model.enums.RequestType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "requests", indexes = {
        @Index(name = "idx_requests_user", columnList = "user_id"),
        @Index(name = "idx_requests_status", columnList = "status")
})
public class Request extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(columnDefinition = "TEXT")
    private String rejectReason;

    public void process(RequestStatus status, User admin, String rejectReason) {
        this.status = status;
        this.admin = admin;
        this.rejectReason = rejectReason;
        this.processedAt = LocalDateTime.now();
    }
}
