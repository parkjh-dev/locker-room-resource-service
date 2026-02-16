package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Board;
import com.lockerroom.resourceservice.model.entity.Comment;
import com.lockerroom.resourceservice.model.entity.Post;
import com.lockerroom.resourceservice.model.entity.User;
import com.lockerroom.resourceservice.model.enums.BoardType;
import com.lockerroom.resourceservice.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.lockerroom.resourceservice.configuration.JpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaConfig.class)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Board board;
    private Post post;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .nickname("testuser")
                .password("password123")
                .role(Role.USER)
                .build();
        entityManager.persist(user);

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
    }

    @Nested
    @DisplayName("findByPostIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtAsc")
    class FindRootComments {

        @Test
        @DisplayName("should return only root-level comments for a post")
        void findRootComments_success() {
            // given
            Comment rootComment1 = Comment.builder()
                    .post(post).user(user)
                    .content("Root comment 1")
                    .build();
            entityManager.persist(rootComment1);

            Comment rootComment2 = Comment.builder()
                    .post(post).user(user)
                    .content("Root comment 2")
                    .build();
            entityManager.persist(rootComment2);

            Comment childComment = Comment.builder()
                    .post(post).user(user)
                    .parent(rootComment1)
                    .content("Reply to root 1")
                    .build();
            entityManager.persist(childComment);

            entityManager.flush();

            // when
            List<Comment> results = commentRepository
                    .findByPostIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(post.getId());

            // then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(Comment::getContent)
                    .containsExactly("Root comment 1", "Root comment 2");
        }

        @Test
        @DisplayName("should exclude soft-deleted root comments")
        void findRootComments_excludesDeleted() {
            // given
            Comment activeComment = Comment.builder()
                    .post(post).user(user)
                    .content("Active comment")
                    .build();
            entityManager.persist(activeComment);

            Comment deletedComment = Comment.builder()
                    .post(post).user(user)
                    .content("Deleted comment")
                    .build();
            entityManager.persist(deletedComment);
            entityManager.flush();

            deletedComment.softDelete();
            entityManager.flush();

            // when
            List<Comment> results = commentRepository
                    .findByPostIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(post.getId());

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getContent()).isEqualTo("Active comment");
        }

        @Test
        @DisplayName("should return empty list when no comments exist")
        void findRootComments_emptyResult() {
            // given - no comments persisted

            // when
            List<Comment> results = commentRepository
                    .findByPostIdAndParentIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(post.getId());

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc")
    class FindChildComments {

        @Test
        @DisplayName("should return child comments for a parent")
        void findChildComments_success() {
            // given
            Comment parentComment = Comment.builder()
                    .post(post).user(user)
                    .content("Parent comment")
                    .build();
            entityManager.persist(parentComment);

            Comment child1 = Comment.builder()
                    .post(post).user(user)
                    .parent(parentComment)
                    .content("Child 1")
                    .build();
            Comment child2 = Comment.builder()
                    .post(post).user(user)
                    .parent(parentComment)
                    .content("Child 2")
                    .build();
            entityManager.persist(child1);
            entityManager.persist(child2);
            entityManager.flush();

            // when
            List<Comment> results = commentRepository
                    .findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(parentComment.getId());

            // then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(Comment::getContent)
                    .containsExactly("Child 1", "Child 2");
        }

        @Test
        @DisplayName("should exclude soft-deleted child comments")
        void findChildComments_excludesDeleted() {
            // given
            Comment parentComment = Comment.builder()
                    .post(post).user(user)
                    .content("Parent comment")
                    .build();
            entityManager.persist(parentComment);

            Comment activeChild = Comment.builder()
                    .post(post).user(user)
                    .parent(parentComment)
                    .content("Active child")
                    .build();
            Comment deletedChild = Comment.builder()
                    .post(post).user(user)
                    .parent(parentComment)
                    .content("Deleted child")
                    .build();
            entityManager.persist(activeChild);
            entityManager.persist(deletedChild);
            entityManager.flush();

            deletedChild.softDelete();
            entityManager.flush();

            // when
            List<Comment> results = commentRepository
                    .findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(parentComment.getId());

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getContent()).isEqualTo("Active child");
        }

        @Test
        @DisplayName("should return empty list when parent has no children")
        void findChildComments_noChildren() {
            // given
            Comment parentComment = Comment.builder()
                    .post(post).user(user)
                    .content("Lonely parent")
                    .build();
            entityManager.persist(parentComment);
            entityManager.flush();

            // when
            List<Comment> results = commentRepository
                    .findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(parentComment.getId());

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByPostIdAndDeletedAtIsNull")
    class CountByPostId {

        @Test
        @DisplayName("should count non-deleted comments for a post")
        void countByPostId_success() {
            // given
            Comment comment1 = Comment.builder()
                    .post(post).user(user)
                    .content("Comment 1")
                    .build();
            Comment comment2 = Comment.builder()
                    .post(post).user(user)
                    .content("Comment 2")
                    .build();
            Comment comment3 = Comment.builder()
                    .post(post).user(user)
                    .content("Comment 3")
                    .build();
            entityManager.persist(comment1);
            entityManager.persist(comment2);
            entityManager.persist(comment3);
            entityManager.flush();

            // when
            int count = commentRepository.countByPostIdAndDeletedAtIsNull(post.getId());

            // then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("should exclude soft-deleted comments from count")
        void countByPostId_excludesDeleted() {
            // given
            Comment activeComment = Comment.builder()
                    .post(post).user(user)
                    .content("Active")
                    .build();
            Comment deletedComment = Comment.builder()
                    .post(post).user(user)
                    .content("Deleted")
                    .build();
            entityManager.persist(activeComment);
            entityManager.persist(deletedComment);
            entityManager.flush();

            deletedComment.softDelete();
            entityManager.flush();

            // when
            int count = commentRepository.countByPostIdAndDeletedAtIsNull(post.getId());

            // then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("should return zero when no comments exist")
        void countByPostId_zeroWhenNoComments() {
            // given - no comments

            // when
            int count = commentRepository.countByPostIdAndDeletedAtIsNull(post.getId());

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("should count both root and child comments")
        void countByPostId_includesChildComments() {
            // given
            Comment rootComment = Comment.builder()
                    .post(post).user(user)
                    .content("Root")
                    .build();
            entityManager.persist(rootComment);

            Comment childComment = Comment.builder()
                    .post(post).user(user)
                    .parent(rootComment)
                    .content("Child")
                    .build();
            entityManager.persist(childComment);
            entityManager.flush();

            // when
            int count = commentRepository.countByPostIdAndDeletedAtIsNull(post.getId());

            // then
            assertThat(count).isEqualTo(2);
        }
    }
}
