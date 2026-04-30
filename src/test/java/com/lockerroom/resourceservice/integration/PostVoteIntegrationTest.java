package com.lockerroom.resourceservice.integration;

import com.lockerroom.resourceservice.board.model.entity.Board;
import com.lockerroom.resourceservice.board.model.enums.BoardType;
import com.lockerroom.resourceservice.integration.support.IntegrationTestBase;
import com.lockerroom.resourceservice.post.model.entity.Poll;
import com.lockerroom.resourceservice.post.model.entity.PollOption;
import com.lockerroom.resourceservice.post.model.entity.Post;
import com.lockerroom.resourceservice.post.model.enums.PostCategory;
import com.lockerroom.resourceservice.post.repository.PollOptionRepository;
import com.lockerroom.resourceservice.post.repository.PollRepository;
import com.lockerroom.resourceservice.post.repository.PollVoteRepository;
import com.lockerroom.resourceservice.post.repository.PostRepository;
import com.lockerroom.resourceservice.user.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시글 투표 통합 테스트 — 멱등성·마감·옵션 검증을 풀 스택에서 확인.
 *
 * <p>Mock 단위테스트와 달리 다음을 실제로 보장:
 * <ul>
 *   <li>{@code uk_poll_votes_poll_user} DB UNIQUE 제약 — saveAndFlush 시 즉시 위반 catch</li>
 *   <li>@Idempotent AOP — Idempotency-Key 헤더 검증 + Redis 캐시 응답 재생</li>
 *   <li>option.voteCount / poll.totalVotes 카운터가 두 번째 호출에서 증가하지 않음</li>
 * </ul>
 */
class PostVoteIntegrationTest extends IntegrationTestBase {

    private static final String VOTE_PATH = "/api/v1/posts/{postId}/vote";

    @Autowired private PostRepository postRepository;
    @Autowired private PollRepository pollRepository;
    @Autowired private PollOptionRepository pollOptionRepository;
    @Autowired private PollVoteRepository pollVoteRepository;

    private Board board;

    @BeforeEach
    void seed() {
        board = createBoard("자유 게시판", BoardType.COMMON);
    }

    @Nested
    @DisplayName("POST /api/v1/posts/{postId}/vote — 멱등성")
    class VoteIdempotency {

        @Test
        @DisplayName("같은 사용자가 두 번 투표해도 vote 1건, 카운터는 +1만 — DB UNIQUE + 서비스 사전 체크가 함께 보호")
        void doubleVoteByService_remainsOne() throws Exception {
            User user = createUser("kc-user-1", "voter");
            Post post = savePostWithPoll(user, "옵션A", "옵션B");
            Poll poll = pollRepository.findByPostId(post.getId()).orElseThrow();
            PollOption option = pollOptionRepository.findByPollIdOrderByIdAsc(poll.getId()).get(0);

            // 첫 호출 — 새 idempotency key
            mockMvc.perform(post(VOTE_PATH, post.getId())
                            .with(jwtFor(user))
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("optionId", option.getId()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalVotes").value(1))
                    .andExpect(jsonPath("$.data.myVoteOptionId").value(option.getId()));

            // 두 번째 호출 — 다른 idempotency key (AOP 우회) — 서비스 레이어 사전 체크가 잡아야 함
            mockMvc.perform(post(VOTE_PATH, post.getId())
                            .with(jwtFor(user))
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("optionId", option.getId()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalVotes").value(1)) // 증가 안 함
                    .andExpect(jsonPath("$.data.myVoteOptionId").value(option.getId()));

            assertThat(pollVoteRepository.findByPollIdAndUserId(poll.getId(), user.getId()))
                    .isPresent();
            assertThat(pollOptionRepository.findById(option.getId()).orElseThrow().getVoteCount())
                    .isEqualTo(1);
            assertThat(pollRepository.findById(poll.getId()).orElseThrow().getTotalVotes())
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("같은 Idempotency-Key로 재호출 시 AOP 캐시가 첫 응답을 재생 — 서비스 미진입")
        void sameIdempotencyKey_returnsCachedResponse() throws Exception {
            User user = createUser("kc-user-1", "voter");
            Post post = savePostWithPoll(user, "A", "B");
            Poll poll = pollRepository.findByPostId(post.getId()).orElseThrow();
            PollOption option = pollOptionRepository.findByPollIdOrderByIdAsc(poll.getId()).get(0);

            String key = UUID.randomUUID().toString();
            String body = objectMapper.writeValueAsString(Map.of("optionId", option.getId()));

            mockMvc.perform(post(VOTE_PATH, post.getId())
                            .with(jwtFor(user))
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());

            // 같은 key 재요청 — Redis(or fallback)에서 캐시된 응답 그대로 재생
            mockMvc.perform(post(VOTE_PATH, post.getId())
                            .with(jwtFor(user))
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalVotes").value(1));

            assertThat(pollOptionRepository.findById(option.getId()).orElseThrow().getVoteCount())
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Idempotency-Key 헤더 누락 시 400 IDEMPOTENCY_KEY_MISSING")
        void missingIdempotencyKey_returns400() throws Exception {
            User user = createUser("kc-user-1", "voter");
            Post post = savePostWithPoll(user, "A", "B");
            Poll poll = pollRepository.findByPostId(post.getId()).orElseThrow();
            PollOption option = pollOptionRepository.findByPollIdOrderByIdAsc(poll.getId()).get(0);

            mockMvc.perform(post(VOTE_PATH, post.getId())
                            .with(jwtFor(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("optionId", option.getId()))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_IDEMPOTENCY_KEY_MISSING"));

            assertThat(pollVoteRepository.findByPollIdAndUserId(poll.getId(), user.getId()))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/posts/{postId}/vote — 마감/유효성")
    class VoteValidation {

        @Test
        @DisplayName("마감 시각이 지난 투표는 400 POLL_EXPIRED")
        void expiredPoll_returns400() throws Exception {
            User user = createUser("kc-user-1", "voter");
            Post post = postRepository.save(Post.builder()
                    .board(board).user(user)
                    .title("title").content("content")
                    .category(PostCategory.GENERAL)
                    .build());
            Poll poll = pollRepository.save(Poll.builder()
                    .post(post)
                    .question("Q?")
                    .expiresAt(LocalDateTime.now().minusMinutes(1)) // 이미 마감
                    .build());
            PollOption option = pollOptionRepository.save(PollOption.builder()
                    .poll(poll).text("A").build());

            mockMvc.perform(post(VOTE_PATH, post.getId())
                            .with(jwtFor(user))
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("optionId", option.getId()))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("POLL_EXPIRED"));

            assertThat(pollVoteRepository.findByPollIdAndUserId(poll.getId(), user.getId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("다른 투표의 옵션 ID로 요청 시 400 POLL_OPTION_INVALID")
        void crossPollOption_returns400() throws Exception {
            User user = createUser("kc-user-1", "voter");
            Post target = savePostWithPoll(user, "A", "B");
            Post other = savePostWithPoll(user, "X", "Y");
            Poll otherPoll = pollRepository.findByPostId(other.getId()).orElseThrow();
            PollOption otherOption = pollOptionRepository
                    .findByPollIdOrderByIdAsc(otherPoll.getId()).get(0);

            mockMvc.perform(post(VOTE_PATH, target.getId())
                            .with(jwtFor(user))
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("optionId", otherOption.getId()))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("POLL_OPTION_INVALID"));
        }

        @Test
        @DisplayName("투표가 없는 게시글에 vote 호출 시 404 POLL_NOT_FOUND")
        void postWithoutPoll_returns404() throws Exception {
            User user = createUser("kc-user-1", "voter");
            Post post = postRepository.save(Post.builder()
                    .board(board).user(user)
                    .title("title").content("content")
                    .category(PostCategory.GENERAL).build());

            mockMvc.perform(post(VOTE_PATH, post.getId())
                            .with(jwtFor(user))
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"optionId\": 9999}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POLL_NOT_FOUND"));
        }
    }

    private Post savePostWithPoll(User user, String optionA, String optionB) {
        Post post = postRepository.save(Post.builder()
                .board(board)
                .user(user)
                .title("title")
                .content("content")
                .category(PostCategory.GENERAL)
                .build());
        Poll poll = pollRepository.save(Poll.builder()
                .post(post)
                .question("Q?")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build());
        pollOptionRepository.save(PollOption.builder().poll(poll).text(optionA).build());
        pollOptionRepository.save(PollOption.builder().poll(poll).text(optionB).build());
        return post;
    }
}
