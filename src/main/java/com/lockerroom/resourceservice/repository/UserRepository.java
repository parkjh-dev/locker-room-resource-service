package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    List<User> findByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);
}
