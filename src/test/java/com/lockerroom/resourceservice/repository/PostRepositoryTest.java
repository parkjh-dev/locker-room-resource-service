package com.lockerroom.resourceservice.repository;

import com.lockerroom.resourceservice.model.entity.Board;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaConfig.class)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Board board;
    private Pageable defaultPageable;

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

        entityManager.flush();
        defaultPageable = PageRequest.of(0, 10);
    }

    @Nested
    @DisplayName("searchByBoard")
    class SearchByBoard {

        @Test
        @DisplayName("should return all posts when keyword is null")
        void searchByBoard_nullKeyword_returnsAllPosts() {
            // given
            Post post1 = Post.builder()
                    .board(board).user(user)
                    .title("First Post").content("Hello world")
                    .build();
            Post post2 = Post.builder()
                    .board(board).user(user)
                    .title("Second Post").content("Goodbye world")
                    .build();
            entityManager.persist(post1);
            entityManager.persist(post2);
            entityManager.flush();

            // when
            List<Post> results = postRepository.searchByBoard(
                    board.getId(), null, null, null, defaultPageable);

            // then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("should search by TITLE search type")
        void searchByBoard_titleSearchType_matchesTitle() {
            // given
            Post matchingPost = Post.builder()
                    .board(board).user(user)
                    .title("Spring Boot Guide").content("Some content")
                    .build();
            Post nonMatchingPost = Post.builder()
                    .board(board).user(user)
                    .title("Java Basics").content("Spring Boot is great")
                    .build();
            entityManager.persist(matchingPost);
            entityManager.persist(nonMatchingPost);
            entityManager.flush();

            // when
            List<Post> results = postRepository.searchByBoard(
                    board.getId(), "TITLE", "Spring", null, defaultPageable);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Spring Boot Guide");
        }

        @Test
        @DisplayName("should search by CONTENT search type")
        void searchByBoard_contentSearchType_matchesContent() {
            // given
            Post matchingPost = Post.builder()
                    .board(board).user(user)
                    .title("Post A").content("Learn Spring Boot today")
                    .build();
            Post nonMatchingPost = Post.builder()
                    .board(board).user(user)
                    .title("Spring Tutorial").content("Learn Java basics")
                    .build();
            entityManager.persist(matchingPost);
            entityManager.persist(nonMatchingPost);
            entityManager.flush();

            // when
            List<Post> results = postRepository.searchByBoard(
                    board.getId(), "CONTENT", "Spring", null, defaultPageable);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Post A");
        }

        @Test
        @DisplayName("should search by TITLE_CONTENT search type")
        void searchByBoard_titleContentSearchType_matchesBoth() {
            // given
            Post titleMatch = Post.builder()
                    .board(board).user(user)
                    .title("Spring Guide").content("Unrelated content")
                    .build();
            Post contentMatch = Post.builder()
                    .board(board).user(user)
                    .title("Unrelated title").content("Spring is powerful")
                    .build();
            Post noMatch = Post.builder()
                    .board(board).user(user)
                    .title("Java Basics").content("Learn Java")
                    .build();
            entityManager.persist(titleMatch);
            entityManager.persist(contentMatch);
            entityManager.persist(noMatch);
            entityManager.flush();

            // when
            List<Post> results = postRepository.searchByBoard(
                    board.getId(), "TITLE_CONTENT", "Spring", null, defaultPageable);

            // then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("should search by NICKNAME search type")
        void searchByBoard_nicknameSearchType_matchesNickname() {
            // given
            User anotherUser = User.builder()
                    .email("another@example.com")
                    .nickname("springfan")
                    .password("password123")
                    .role(Role.USER)
                    .build();
            entityManager.persist(anotherUser);

            Post postByTestUser = Post.builder()
                    .board(board).user(user)
                    .title("Post 1").content("Content 1")
                    .build();
            Post postByAnotherUser = Post.builder()
                    .board(board).user(anotherUser)
                    .title("Post 2").content("Content 2")
                    .build();
            entityManager.persist(postByTestUser);
            entityManager.persist(postByAnotherUser);
            entityManager.flush();

            // when
            List<Post> results = postRepository.searchByBoard(
                    board.getId(), "NICKNAME", "springfan", null, defaultPageable);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getUser().getNickname()).isEqualTo("springfan");
        }

        @Test
        @DisplayName("should exclude soft-deleted posts")
        void searchByBoard_excludesSoftDeletedPosts() {
            // given
            Post activePost = Post.builder()
                    .board(board).user(user)
                    .title("Active Post").content("Still here")
                    .build();
            Post deletedPost = Post.builder()
                    .board(board).user(user)
                    .title("Deleted Post").content("Gone")
                    .build();
            entityManager.persist(activePost);
            entityManager.persist(deletedPost);
            entityManager.flush();

            deletedPost.softDelete();
            entityManager.flush();

            // when
            List<Post> results = postRepository.searchByBoard(
                    board.getId(), null, null, null, defaultPageable);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Active Post");
        }

        @Test
        @DisplayName("should only return posts for the specified board")
        void searchByBoard_filtersbyBoardId() {
            // given
            Board otherBoard = Board.builder()
                    .name("Other Board")
                    .type(BoardType.QNA)
                    .build();
            entityManager.persist(otherBoard);

            Post postOnBoard = Post.builder()
                    .board(board).user(user)
                    .title("On Board").content("Content")
                    .build();
            Post postOnOtherBoard = Post.builder()
                    .board(otherBoard).user(user)
                    .title("On Other Board").content("Content")
                    .build();
            entityManager.persist(postOnBoard);
            entityManager.persist(postOnOtherBoard);
            entityManager.flush();

            // when
            List<Post> results = postRepository.searchByBoard(
                    board.getId(), null, null, null, defaultPageable);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("On Board");
        }

        @Test
        @DisplayName("should respect pagination")
        void searchByBoard_pagination_returnsLimitedResults() {
            // given
            for (int i = 0; i < 5; i++) {
                Post post = Post.builder()
                        .board(board).user(user)
                        .title("Post " + i).content("Content " + i)
                        .build();
                entityManager.persist(post);
            }
            entityManager.flush();

            Pageable pageOf2 = PageRequest.of(0, 2);

            // when
            List<Post> results = postRepository.searchByBoard(
                    board.getId(), null, null, null, pageOf2);

            // then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findByUserIdAndDeletedAtIsNullOrderByIdDesc")
    class FindByUserIdAndDeletedAtIsNull {

        @Test
        @DisplayName("should return non-deleted posts by user")
        void findByUserIdAndDeletedAtIsNull_success() {
            // given
            Post post1 = Post.builder()
                    .board(board).user(user)
                    .title("Post 1").content("Content 1")
                    .build();
            Post post2 = Post.builder()
                    .board(board).user(user)
                    .title("Post 2").content("Content 2")
                    .build();
            entityManager.persist(post1);
            entityManager.persist(post2);
            entityManager.flush();

            // when
            List<Post> results = postRepository
                    .findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                            user.getId(), defaultPageable);

            // then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("should exclude soft-deleted posts")
        void findByUserIdAndDeletedAtIsNull_excludesDeleted() {
            // given
            Post activePost = Post.builder()
                    .board(board).user(user)
                    .title("Active").content("Content")
                    .build();
            Post deletedPost = Post.builder()
                    .board(board).user(user)
                    .title("Deleted").content("Content")
                    .build();
            entityManager.persist(activePost);
            entityManager.persist(deletedPost);
            entityManager.flush();

            deletedPost.softDelete();
            entityManager.flush();

            // when
            List<Post> results = postRepository
                    .findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                            user.getId(), defaultPageable);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Active");
        }

        @Test
        @DisplayName("should not return posts of other users")
        void findByUserIdAndDeletedAtIsNull_onlyOwnPosts() {
            // given
            User otherUser = User.builder()
                    .email("other@example.com")
                    .nickname("otheruser")
                    .password("password123")
                    .role(Role.USER)
                    .build();
            entityManager.persist(otherUser);

            Post myPost = Post.builder()
                    .board(board).user(user)
                    .title("My Post").content("Content")
                    .build();
            Post otherPost = Post.builder()
                    .board(board).user(otherUser)
                    .title("Other Post").content("Content")
                    .build();
            entityManager.persist(myPost);
            entityManager.persist(otherPost);
            entityManager.flush();

            // when
            List<Post> results = postRepository
                    .findByUserIdAndDeletedAtIsNullOrderByIdDesc(
                            user.getId(), defaultPageable);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("My Post");
        }
    }
}
