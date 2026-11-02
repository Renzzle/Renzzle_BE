package com.renzzle.backend.domain.puzzle.community.api;

import com.jayway.jsonpath.JsonPath;
import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.auth.service.JwtProvider;
import com.renzzle.backend.domain.puzzle.cache.dao.PuzzleCacheRepository;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.service.PuzzleCacheService;
import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.service.CommunityShareService;
import com.renzzle.backend.domain.puzzle.rank.support.TestUserFactory;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.security.AppKeyAuthenticationFilter;
import com.renzzle.backend.support.SharedMovesTestDecoder;
import com.renzzle.backend.support.TestCommunityPuzzleBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.renzzle.backend.support.TestTime.FIXED_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class CommunityShareIntegrationTest {

    // Black h8-h10 against white i8-i10; black h11, white h12, black h7 makes five
    private static final String BOARD = "h8i8h9i9h10i10";
    private static final String ANSWER = "h11h12h7";
    private static final int H11 = 115;
    private static final int H12 = 116;
    private static final int VIEWS = 3;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityPuzzleRepository communityPuzzleRepository;
    @Autowired private PuzzleCacheRepository puzzleCacheRepository;
    @Autowired private PuzzleCacheService puzzleCacheService;
    @Autowired private CommunityShareService communityShareService;
    @Autowired private PlatformTransactionManager transactionManager;

    private UserEntity user;
    private Long puzzleId;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
                TestUserFactory.createTestUser("share-" + UUID.randomUUID().toString().substring(0, 8), 1000));
        puzzleId = TestCommunityPuzzleBuilder.builder(user)
                .withBoardStatus(BOARD)
                .withAnswer(ANSWER)
                .withView(VIEWS)
                .save(communityPuzzleRepository)
                .getId();
    }

    @AfterEach
    void tearDown() {
        puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(PuzzleType.COMMUNITY, puzzleId)
                .ifPresent(puzzleCacheRepository::delete);
        // Deleting the author cascades to the puzzle, soft-deleted or not
        userRepository.delete(user);
    }

    @Test
    void share_WhenCalledAgain_ThenReturnsTheSameLink() throws Exception {
        String first = share();
        String second = share();

        assertThat(first).matches("https://renzzle\\.com/puzzle/[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(second).isEqualTo(first);
    }

    @Test
    void share_WhenTwoFirstSharesRace_ThenBothGetTheSameLink() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> shares = IntStream.range(0, 4)
                    .<Callable<String>>mapToObj(i -> () -> communityShareService.shareCommunityPuzzle(puzzleId).shareUrl())
                    .toList();
            Set<String> links = executor.invokeAll(shares).stream()
                    .map(this::getUnchecked)
                    .collect(Collectors.toSet());

            assertThat(links).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void share_WhenPuzzleDeleted_ThenNotFound() throws Exception {
        softDeletePuzzle();

        mockMvc.perform(asUser(post("/api/community/puzzle/" + puzzleId + "/share")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorResponse.code").value(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE.getCode()));
    }

    @Test
    void sharedPuzzleId_InTheApp_ThenReturnsThePuzzleBehindTheToken() throws Exception {
        String token = tokenOf(share());

        mockMvc.perform(asUser(get("/api/community/puzzle/share/" + token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.puzzleId").value(puzzleId));
    }

    @Test
    void sharedPuzzle_WithoutLoginOrAppKey_ThenReturnsTheBoardWithoutTheAnswer() throws Exception {
        String token = tokenOf(share());

        mockMvc.perform(get("/puzzle/" + token + "/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.boardStatus").value(BOARD))
                .andExpect(jsonPath("$.response.authorName").value(user.getNickname()))
                .andExpect(jsonPath("$.response.answer").doesNotExist())
                .andExpect(jsonPath("$.response.id").doesNotExist())
                .andExpect(jsonPath("$.response.puzzleId").doesNotExist());

        assertThat(communityPuzzleRepository.findById(puzzleId).orElseThrow().getView()).isEqualTo(VIEWS);
    }

    @Test
    void sharedPuzzle_WhenTokenUnknown_ThenNotFound() throws Exception {
        mockMvc.perform(get("/puzzle/" + UUID.randomUUID() + "/data"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorResponse.code").value(ErrorCode.CANNOT_FIND_COMMUNITY_PUZZLE.getCode()));
    }

    @Test
    void sharedPuzzle_WhenDeletedAfterSharing_ThenNotFound() throws Exception {
        String token = tokenOf(share());
        softDeletePuzzle();

        mockMvc.perform(get("/puzzle/" + token + "/data"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/puzzle/" + token + "/next-moves").param("board", BOARD))
                .andExpect(status().isNotFound());
    }

    @Test
    void sharedNextMoves_WithoutLoginOrAppKey_ThenDecodesToTheCachedReplies() throws Exception {
        puzzleCacheService.seedSolutionPath(PuzzleType.COMMUNITY, puzzleId, BOARD, ANSWER);
        String token = tokenOf(share());

        String body = mockMvc.perform(get("/puzzle/" + token + "/next-moves").param("board", BOARD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String encoded = JsonPath.read(body, "$.response");
        assertThat(encoded).doesNotContain("h11", "h12");
        assertThat(SharedMovesTestDecoder.decode(encoded, token, BOARD)).isEqualTo(Map.of(H11, H12));
    }

    @Test
    void sharedNextMoves_WhenBoardMissing_ThenBadRequest() throws Exception {
        String token = tokenOf(share());

        mockMvc.perform(get("/puzzle/" + token + "/next-moves"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sharedPuzzle_WhenWritten_ThenStillNeedsLogin() throws Exception {
        String token = tokenOf(share());

        mockMvc.perform(post("/puzzle/" + token + "/data"))
                .andExpect(status().isUnauthorized());
    }

    private String share() throws Exception {
        String body = mockMvc.perform(asUser(post("/api/community/puzzle/" + puzzleId + "/share")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.response.shareUrl");
    }

    private String tokenOf(String shareUrl) {
        return shareUrl.substring(shareUrl.lastIndexOf('/') + 1);
    }

    private void softDeletePuzzle() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                communityPuzzleRepository.softDelete(puzzleId, FIXED_INSTANT));
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(user.getId(), "test-session"))
                .header(AppKeyAuthenticationFilter.APP_KEY_HEADER, "test-app-key");
    }

    private String getUnchecked(Future<String> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
