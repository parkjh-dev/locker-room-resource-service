package com.lockerroom.resourceservice.user.repository;

import com.lockerroom.resourceservice.user.model.entity.UserWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWithdrawalRepository extends JpaRepository<UserWithdrawal, Long> {
}
