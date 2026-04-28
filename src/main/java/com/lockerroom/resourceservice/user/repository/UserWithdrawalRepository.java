package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.UserWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWithdrawalRepository extends JpaRepository<UserWithdrawal, Long> {
}
