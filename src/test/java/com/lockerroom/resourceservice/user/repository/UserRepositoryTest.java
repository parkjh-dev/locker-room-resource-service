package com.lockerroom.resourceservice.user.repository;

import com.lockerroom.resourceservice.user.repository.UserRepository;

import com.lockerroom.resourceservice.user.model.entity.User;
import com.lockerroom.resourceservice.common.model.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.lockerroom.resourceservice.infrastructure.configuration.JpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("should find user by email when exists")
        void findByEmail_success() {
            // given
            User user = User.builder()
                    .email("john@example.com")
                    .nickname("john")
                    .password("password123")
                    .role(Role.USER)
                    .build();
            entityManager.persist(user);
            entityManager.flush();

            // when
            Optional<User> result = userRepository.findByEmail("john@example.com");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("john@example.com");
            assertThat(result.get().getNickname()).isEqualTo("john");
        }

        @Test
        @DisplayName("should return empty when email does not exist")
        void findByEmail_notFound() {
            // given - no user persisted

            // when
            Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should match exact email only")
        void findByEmail_exactMatch() {
            // given
            User user = User.builder()
                    .email("test@example.com")
                    .nickname("testuser")
                    .password("password123")
                    .role(Role.USER)
                    .build();
            entityManager.persist(user);
            entityManager.flush();

            // when
            Optional<User> result = userRepository.findByEmail("test@example.co");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("should return true when email exists")
        void existsByEmail_true() {
            // given
            User user = User.builder()
                    .email("existing@example.com")
                    .nickname("existing")
                    .password("password123")
                    .role(Role.USER)
                    .build();
            entityManager.persist(user);
            entityManager.flush();

            // when
            boolean exists = userRepository.existsByEmail("existing@example.com");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when email does not exist")
        void existsByEmail_false() {
            // given - no user persisted

            // when
            boolean exists = userRepository.existsByEmail("absent@example.com");

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByNickname")
    class ExistsByNickname {

        @Test
        @DisplayName("should return true when nickname exists")
        void existsByNickname_true() {
            // given
            User user = User.builder()
                    .email("user@example.com")
                    .nickname("uniquenick")
                    .password("password123")
                    .role(Role.USER)
                    .build();
            entityManager.persist(user);
            entityManager.flush();

            // when
            boolean exists = userRepository.existsByNickname("uniquenick");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when nickname does not exist")
        void existsByNickname_false() {
            // given - no user persisted

            // when
            boolean exists = userRepository.existsByNickname("nonexistentnick");

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should distinguish different nicknames")
        void existsByNickname_differentNickname() {
            // given
            User user = User.builder()
                    .email("user@example.com")
                    .nickname("alpha")
                    .password("password123")
                    .role(Role.USER)
                    .build();
            entityManager.persist(user);
            entityManager.flush();

            // when
            boolean existsAlpha = userRepository.existsByNickname("alpha");
            boolean existsBeta = userRepository.existsByNickname("beta");

            // then
            assertThat(existsAlpha).isTrue();
            assertThat(existsBeta).isFalse();
        }
    }
}
