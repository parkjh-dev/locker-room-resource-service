package com.lockerroom.resourceservice.model.entity;

import com.lockerroom.resourceservice.model.enums.InquiryStatus;
import com.lockerroom.resourceservice.model.enums.InquiryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "inquiries", indexes = {
        @Index(name = "idx_inquiries_user", columnList = "user_id"),
        @Index(name = "idx_inquiries_status", columnList = "status")
})
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryStatus status = InquiryStatus.PENDING;

    public void updateStatus(InquiryStatus status) {
        this.status = status;
    }
}
