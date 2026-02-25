package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    List<User> findByDeletedAtIsNullOrderByIdDesc(Pageable pageable);

    List<User> findByDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL " +
           "AND (:keyword IS NULL OR u.nickname LIKE CONCAT('%', :keyword, '%') OR u.email LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:cursorId IS NULL OR u.id < :cursorId) " +
           "ORDER BY u.id DESC")
    List<User> findUsersFiltered(@Param("keyword") String keyword,
                                 @Param("role") Role role,
                                 @Param("cursorId") Long cursorId,
                                 Pageable pageable);
}
