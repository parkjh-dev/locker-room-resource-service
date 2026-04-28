package com.lockerroom.resourceservice.inquiry.model.entity;

import com.lockerroom.resourceservice.user.model.entity.User;

import com.lockerroom.resourceservice.inquiry.model.entity.Inquiry;

import com.lockerroom.resourceservice.common.model.entity.BaseEntity;

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
@Table(name = "inquiry_replies", indexes = {
        @Index(name = "idx_inquiry_replies_inquiry", columnList = "inquiry_id")
})
public class InquiryReply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
