package com.lockerroom.resourceservice.post.repository;

import com.lockerroom.resourceservice.post.repository.PostLikeRepository;

import com.lockerroom.resourceservice.board.model.entity.Board;
import com.lockerroom.resourceservice.post.model.entity.Post;
import com.lockerroom.resourceservice.post.model.entity.PostLike;
import com.lockerroom.resourceservice.user.model.entity.User;
import com.lockerroom.resourceservice.board.model.enums.BoardType;
import com.lockerroom.resourceservice.common.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.lockerroom.resourceservice.infrastructure.configuration.JpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaConfig.class)
class PostLikeRepositoryTest {

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private User anotherUser;
    private Board board;
    private Post post;
    private Pageable defaultPageable;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("user@example.com")
                .nickname("user1")
                .password("password123")
                .role(Role.USER)
                .build();
        entityManager.persist(user);

        anotherUser = User.builder()
                .email("another@example.com")
                .nickname("user2")
                .password("password123")
                .role(Role.USER)
                .build();
        entityManager.persist(anotherUser);

        board = Board.builder()
                .name("General Board")
                .type(BoardType.COMMON)
                .build();
        entityManager.persist(board);

        post = Post.builder()
                .board(board).user(user)
                .title("Test Post").content("Test Content")
                .build();
        entityManager.persist(post);

        entityManager.flush();
        defaultPageable = PageRequest.of(0, 10);
    }

    @Nested
    @DisplayName("findByPostIdAndUserId")
    class FindByPostIdAndUserId {

        @Test
        @DisplayName("should find like when user has liked the post")
        void findByPostIdAndUserId_success() {
            // given
            PostLike like = PostLike.builder()
                    .post(post).user(user)
                    .build();
            entityManager.persist(like);
            entityManager.flush();

            // when
            Optional<PostLike> result = postLikeRepository
                    .findByPostIdAndUserId(post.getId(), user.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getPost().getId()).isEqualTo(post.getId());
            assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("should return empty when user has not liked the post")
        void findByPostIdAndUserId_notFound() {
            // given - no like persisted

            // when
            Optional<PostLike> result = postLikeRepository
                    .findByPostIdAndUserId(post.getId(), user.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should distinguish different users' likes")
        void findByPostIdAndUserId_differentUser() {
            // given
            PostLike like = PostLike.builder()
                    .post(post).user(user)
                    .build();
            entityManager.persist(like);
            entityManager.flush();

            // when
            Optional<PostLike> result = postLikeRepository
                    .findByPostIdAndUserId(post.getId(), anotherUser.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByPostIdAndUserId")
    class ExistsByPostIdAndUserId {

        @Test
        @DisplayName("should return true when like exists")
        void existsByPostIdAndUserId_true() {
            // given
            PostLike like = PostLike.builder()
                    .post(post).user(user)
                    .build();
            entityManager.persist(like);
            entityManager.flush();

            // when
            boolean exists = postLikeRepository
                    .existsByPostIdAndUserId(post.getId(), user.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when like does not exist")
        void existsByPostIdAndUserId_false() {
            // given - no like persisted

            // when
            boolean exists = postLikeRepository
                    .existsByPostIdAndUserId(post.getId(), user.getId());

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("countByPostId")
    class CountByPostId {

        @Test
        @DisplayName("should count likes for a post")
        void countByPostId_success() {
            // given
            PostLike like1 = PostLike.builder()
                    .post(post).user(user)
                    .build();
            PostLike like2 = PostLike.builder()
                    .post(post).user(anotherUser)
                    .build();
            entityManager.persist(like1);
            entityManager.persist(like2);
            entityManager.flush();

            // when
            int count = postLikeRepository.countByPostId(post.getId());

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should return zero when no likes exist")
        void countByPostId_zeroLikes() {
            // given - no likes persisted

            // when
            int count = postLikeRepository.countByPostId(post.getId());

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("should only count likes for the specified post")
        void countByPostId_differentPosts() {
            // given
            Post anotherPost = Post.builder()
                    .board(board).user(user)
                    .title("Another Post").content("Another Content")
                    .build();
            entityManager.persist(anotherPost);

            PostLike likeOnPost = PostLike.builder()
                    .post(post).user(user)
                    .build();
            PostLike likeOnAnotherPost = PostLike.builder()
                    .post(anotherPost).user(anotherUser)
                    .build();
            entityManager.persist(likeOnPost);
            entityManager.persist(likeOnAnotherPost);
            entityManager.flush();

            // when
            int count = postLikeRepository.countByPostId(post.getId());

            // then
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findByUserIdWithPost")
    class FindByUserIdWithPost {

        @Test
        @DisplayName("should return likes with fetched post, board, and user")
        void findByUserIdWithPost_success() {
            // given
            PostLike like = PostLike.builder()
                    .post(post).user(user)
                    .build();
            entityManager.persist(like);
            entityManager.flush();
            entityManager.clear();

            // when
            List<PostLike> results = postLikeRepository
                    .findByUserIdWithPost(user.getId(), null, defaultPageable);

            // then
            assertThat(results).hasSize(1);
            PostLike resultLike = results.get(0);
            assertThat(resultLike.getPost()).isNotNull();
            assertThat(resultLike.getPost().getTitle()).isEqualTo("Test Post");
            assertThat(resultLike.getPost().getBoard()).isNotNull();
            assertThat(resultLike.getPost().getBoard().getName()).isEqualTo("General Board");
            assertThat(resultLike.getPost().getUser()).isNotNull();
        }

        @Test
        @DisplayName("should exclude likes on soft-deleted posts")
        void findByUserIdWithPost_excludesDeletedPosts() {
            // given
            Post deletedPost = Post.builder()
                    .board(board).user(user)
                    .title("Deleted Post").content("Content")
                    .build();
            entityManager.persist(deletedPost);
            entityManager.flush();

            PostLike likeOnActive = PostLike.builder()
                    .post(post).user(user)
                    .build();
            PostLike likeOnDeleted = PostLike.builder()
                    .post(deletedPost).user(user)
                    .build();
            entityManager.persist(likeOnActive);
            entityManager.persist(likeOnDeleted);
            entityManager.flush();

            deletedPost.softDelete();
            entityManager.flush();
            entityManager.clear();

            // when
            List<PostLike> results = postLikeRepository
                    .findByUserIdWithPost(user.getId(), null, defaultPageable);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getPost().getTitle()).isEqualTo("Test Post");
        }

        @Test
        @DisplayName("should return empty list when user has no likes")
        void findByUserIdWithPost_noLikes() {
            // given - no likes persisted

            // when
            List<PostLike> results = postLikeRepository
                    .findByUserIdWithPost(user.getId(), null, defaultPageable);

            // then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should only return likes for the specified user")
        void findByUserIdWithPost_onlyOwnLikes() {
            // given
            PostLike userLike = PostLike.builder()
                    .post(post).user(user)
                    .build();
            entityManager.persist(userLike);

            Post anotherPost = Post.builder()
                    .board(board).user(anotherUser)
                    .title("Another Post").content("Content")
                    .build();
            entityManager.persist(anotherPost);

            PostLike anotherUserLike = PostLike.builder()
                    .post(anotherPost).user(anotherUser)
                    .build();
            entityManager.persist(anotherUserLike);
            entityManager.flush();
            entityManager.clear();

            // when
            List<PostLike> results = postLikeRepository
                    .findByUserIdWithPost(user.getId(), null, defaultPageable);

            // then
            assertThat(results).hasSize(1);
        }
    }
}
