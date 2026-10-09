package com.renzzle.backend.domain.puzzle.training.service;

import com.renzzle.backend.domain.appinfo.service.AppInfoService;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.cache.dao.PuzzleCacheRepository;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.service.PuzzleCacheService;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import com.renzzle.backend.domain.puzzle.shared.util.RatingUtil;
import com.renzzle.backend.domain.puzzle.training.api.request.*;
import com.renzzle.backend.domain.puzzle.training.api.response.*;
import com.renzzle.backend.domain.puzzle.training.dao.*;
import com.renzzle.backend.domain.puzzle.training.domain.*;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.constant.ItemPrice;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.renzzle.backend.support.TestTime.FIXED_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrainingServiceTest {

    @Mock
    private PackRepository packRepository;

    @Mock
    private PackTranslationRepository packTranslationRepository;

    @Mock
    private TrainingPuzzleRepository trainingPuzzleRepository;

    @Mock
    private SolvedTrainingPuzzleRepository solvedTrainingPuzzleRepository;

    @Mock
    private UserPackRepository userPackRepository;

    @Mock
    private PuzzleCacheRepository puzzleCacheRepository;

    @Mock
    private PuzzleCacheService puzzleCacheService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @Mock
    private AppInfoService appInfoService;

    @InjectMocks
    private TrainingService trainingService;

    @BeforeEach
    void setup() {
        lenient().when(appInfoService.getPrice(any())).thenAnswer(invocation -> invocation.<ItemPrice>getArgument(0).getDefaultPrice());
    }

    @Nested
    class Success {

        @Test
        void createPack_WhenValidRequest_ThenSavesPack() {
            // given
            List<PackTranslationRequest> translationRequests = Arrays.asList(
                    new PackTranslationRequest("KO", "초보용 1", "강상민", "처음 퍼즐을 푸는..."),
                    new PackTranslationRequest("EN", "For Beginner 1", "Kang Sang-Min", "First time to solve...")
            );
            CreateTrainingPackRequest request = new CreateTrainingPackRequest(translationRequests, 1000, "LOW");

            Pack savedPack = Pack.builder()
                    .id(1L)
                    .price(request.price())
                    .difficulty(Difficulty.getDifficulty(request.difficulty()))
                    .puzzleCount(0)
                    .build();

            when(packRepository.save(any(Pack.class))).thenReturn(savedPack);

            List<PackTranslation> expectedTranslations = request.info().stream()
                    .map(info -> PackTranslation.builder()
                            .pack(savedPack)
                            .langCode(LangCode.getLangCode(info.langCode()))
                            .title(info.title())
                            .author(info.author())
                            .description(info.description())
                            .build())
                    .toList();

            when(packTranslationRepository.saveAll(anyList())).thenReturn(expectedTranslations);

            // when
            Pack result = trainingService.createPack(request);

            // then
            assertNotNull(result);
            assertEquals(savedPack.getId(), result.getId());
            verify(packRepository, times(1)).save(any(Pack.class));
            verify(packTranslationRepository, times(1)).saveAll(anyList());
        }

        @Test
        void updatePack_WhenValidRequest_ThenUpdatesTranslationsPriceAndDifficulty() {
            // given
            Long packId = 1L;
            Pack existingPack = Pack.builder()
                    .id(packId)
                    .price(500)
                    .difficulty(Difficulty.getDifficulty("LOW"))
                    .puzzleCount(7)
                    .build();

            List<PackTranslationRequest> translationRequests = Arrays.asList(
                    new PackTranslationRequest("KO", "초보용 1(수정)", "김", "설명(수정)"),
                    new PackTranslationRequest("EN", "For Beginner 1 (Edited)", "KIM", "Description (Edited)")
            );
            UpdateTrainingPackRequest request = new UpdateTrainingPackRequest(translationRequests, 1200, "HIGH");

            List<PackTranslation> existingTranslations = Arrays.asList(
                    PackTranslation.builder()
                            .id(10L)
                            .pack(existingPack)
                            .langCode(LangCode.getLangCode("KO"))
                            .title("초보용 1")
                            .author("김")
                            .description("처음 퍼즐을 푸는...")
                            .build(),
                    PackTranslation.builder()
                            .id(11L)
                            .pack(existingPack)
                            .langCode(LangCode.getLangCode("EN"))
                            .title("For Beginner 1")
                            .author("KIM")
                            .description("First time to solve...")
                            .build()
            );

            when(packRepository.findById(packId)).thenReturn(Optional.of(existingPack));
            when(packTranslationRepository.findAllByPack_Id(packId)).thenReturn(existingTranslations);
            when(packRepository.save(any(Pack.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Pack updatedPack = trainingService.updatePack(packId, request);

            // then
            assertThat(updatedPack.getId()).isEqualTo(packId);
            assertThat(updatedPack.getPrice()).isEqualTo(1200);
            assertThat(updatedPack.getDifficulty().getName()).isEqualTo("HIGH");
            assertThat(updatedPack.getPuzzleCount()).isEqualTo(7);

            ArgumentCaptor<Pack> packCaptor = ArgumentCaptor.forClass(Pack.class);
            verify(packRepository, times(1)).save(packCaptor.capture());
            assertThat(packCaptor.getValue().getId()).isEqualTo(packId);
            assertThat(packCaptor.getValue().getPrice()).isEqualTo(1200);
            assertThat(packCaptor.getValue().getDifficulty().getName()).isEqualTo("HIGH");
            assertThat(packCaptor.getValue().getPuzzleCount()).isEqualTo(7);

            verify(packTranslationRepository, times(1)).findAllByPack_Id(packId);
            verify(packTranslationRepository, times(1)).deleteAll(existingTranslations);

            ArgumentCaptor<List<PackTranslation>> translationsCaptor = ArgumentCaptor.captor();
            verify(packTranslationRepository, times(1)).saveAll(translationsCaptor.capture());
            List<PackTranslation> savedTranslations = translationsCaptor.getValue();
            assertThat(savedTranslations).hasSize(2).allSatisfy(t -> assertThat(t.getPack().getId()).isEqualTo(packId));

            PackTranslation ko = savedTranslations.stream()
                    .filter(t -> t.getLangCode().getName().equals("KO"))
                    .findFirst()
                    .orElseThrow();
            assertThat(ko.getTitle()).isEqualTo("초보용 1(수정)");
            assertThat(ko.getAuthor()).isEqualTo("김");
            assertThat(ko.getDescription()).isEqualTo("설명(수정)");

            PackTranslation en = savedTranslations.stream()
                    .filter(t -> t.getLangCode().getName().equals("EN"))
                    .findFirst()
                    .orElseThrow();
            assertThat(en.getTitle()).isEqualTo("For Beginner 1 (Edited)");
            assertThat(en.getAuthor()).isEqualTo("KIM");
            assertThat(en.getDescription()).isEqualTo("Description (Edited)");
        }

        @Test
        void addTranslation_WhenPackExists_ThenSavesTranslation() {
            // given
            Long packId = 1L;
            Pack pack = Pack.builder()
                    .id(packId)
                    .puzzleCount(0)
                    .price(1000)
                    .difficulty(Difficulty.getDifficulty("LOW"))
                    .build();

            TranslationRequest request = new TranslationRequest(
                    packId,
                    "EN",
                    "For Beginner 1",
                    "Kang Sang-Min",
                    "First time to solve..."
            );

            when(packRepository.findById(packId)).thenReturn(Optional.of(pack));
            when(packTranslationRepository.existsByPackAndLangCode(
                    eq(pack),
                    argThat(langCode -> langCode != null && langCode.getName().equals("EN"))
            )).thenReturn(false);

            // when
            trainingService.addTranslation(request);

            // Capture the PackTranslation object passed to addTranslation via ArgumentCaptor
            ArgumentCaptor<PackTranslation> captor = ArgumentCaptor.forClass(PackTranslation.class);

            // then
            verify(packTranslationRepository, times(1)).save(captor.capture());

            PackTranslation savedTranslation = captor.getValue();
            assertEquals(pack, savedTranslation.getPack());
            assertEquals(request.langCode(), savedTranslation.getLangCode().getName());
            assertEquals(request.title(), savedTranslation.getTitle());
            assertEquals(request.author(), savedTranslation.getAuthor());
            assertEquals(request.description(), savedTranslation.getDescription());
        }

        @Test
        void createTrainingPuzzle_WhenPackExists_ThenCreatesPuzzle() {
            // given
            Long packId = 1L;
            String boardStatus = "a1a2a3a4";
            Integer depth = 3;
            String winColorStr = "WHITE";
            AddTrainingPuzzleRequest request = new AddTrainingPuzzleRequest(
                    packId,
                    6,
                    boardStatus,
                    "a1a2a3a4",
                    depth,
                    winColorStr
            );

            when(trainingPuzzleRepository.findTopIndex(packId)).thenReturn(5);

            // Create the Pack object (fill in only the required fields)
            Pack pack = Pack.builder()
                    .id(packId)
                    .puzzleCount(0)    // initial puzzleCount value (example)
                    .price(1000)
                    .difficulty(Difficulty.getDifficulty("LOW"))
                    .build();
            when(packRepository.findById(packId)).thenReturn(Optional.of(pack));

            // increasePuzzleCount is a void method, so handle it with doNothing()
            doNothing().when(packRepository).increasePuzzleCount(packId);

            TrainingPuzzle savedPuzzle = TrainingPuzzle.builder()
                    .id(100L)
                    .pack(pack)
                    .trainingIndex(6)
                    .answer("a1a2a3a4")
                    .boardStatus(boardStatus)
                    .boardKey("generatedKey")
                    .depth(depth)
                    .rating(RatingUtil.puzzleRating(depth, WinColor.getWinColor(winColorStr), pack.getDifficulty()))
                    .winColor(WinColor.getWinColor(winColorStr))
                    .build();

            when(trainingPuzzleRepository.save(any(TrainingPuzzle.class))).thenReturn(savedPuzzle);

            // when
            TrainingPuzzle result = trainingService.createTrainingPuzzle(request);

            // then
            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals("a1a2a3a4", result.getAnswer());
            assertEquals(6, result.getTrainingIndex());
            assertEquals(boardStatus, result.getBoardStatus());
            assertEquals("generatedKey", result.getBoardKey());
            assertEquals(depth, result.getDepth());
            assertEquals(RatingUtil.puzzleRating(depth, WinColor.getWinColor(winColorStr), pack.getDifficulty()), result.getRating());

            verify(trainingPuzzleRepository, times(1)).findTopIndex(packId);
            verify(packRepository, times(1)).findById(packId);
            verify(packRepository, times(1)).increasePuzzleCount(packId);
            verify(trainingPuzzleRepository, times(1)).save(any(TrainingPuzzle.class));
        }

        @Test
        void modifyTrainingPuzzle_WhenDepthChanges_ThenRecalculatesRatingAndResetsRankAttempts() {
            // given
            Pack pack = pack(1L, "HIGH");
            TrainingPuzzle existing = trainingPuzzle(10L, pack, 5, 1234.5, 12);
            givenModifiable(existing, pack);

            // when
            TrainingPuzzle modified = trainingService.modifyTrainingPuzzle(10L, new ModifyTrainingPuzzleRequest(
                    1L, null, existing.getBoardStatus(), existing.getAnswer(), 7, "BLACK"));

            // then
            assertThat(modified.getRating())
                    .isEqualTo(RatingUtil.puzzleRating(7, WinColor.getWinColor("BLACK"), pack.getDifficulty()));
            assertThat(modified.getRankAttemptCount()).isZero();
        }

        @Test
        void modifyTrainingPuzzle_WhenAnswerChangesAtSameDepth_ThenStillRecalculatesRating() {
            // given
            Pack pack = pack(1L, "MIDDLE");
            TrainingPuzzle existing = trainingPuzzle(10L, pack, 5, 1234.5, 12);
            givenModifiable(existing, pack);

            // when
            TrainingPuzzle modified = trainingService.modifyTrainingPuzzle(10L, new ModifyTrainingPuzzleRequest(
                    1L, null, existing.getBoardStatus(), "b3", 5, "BLACK"));

            // then
            assertThat(modified.getRating())
                    .isEqualTo(RatingUtil.puzzleRating(5, WinColor.getWinColor("BLACK"), pack.getDifficulty()));
            assertThat(modified.getRankAttemptCount()).isZero();
        }

        @Test
        void modifyTrainingPuzzle_WhenAdminResendsSameValues_ThenKeepsLearnedRating() {
            // given: the admin page sends every field on each save
            Pack pack = pack(1L, "MIDDLE");
            TrainingPuzzle existing = trainingPuzzle(10L, pack, 5, 1234.5, 12);
            givenModifiable(existing, pack);

            // when
            TrainingPuzzle modified = trainingService.modifyTrainingPuzzle(10L, new ModifyTrainingPuzzleRequest(
                    1L, null, existing.getBoardStatus(), existing.getAnswer(), 5, "BLACK"));

            // then
            assertThat(modified.getRating()).isEqualTo(1234.5);
            assertThat(modified.getRankAttemptCount()).isEqualTo(12);
        }

        @Test
        void modifyTrainingPuzzle_WhenBoardAndAnswerUnchanged_ThenKeepsTheCache() {
            // given: the admin page sends every field on each save
            Pack pack = pack(1L, "MIDDLE");
            TrainingPuzzle existing = trainingPuzzle(10L, pack, 5, 1234.5, 12);
            givenModifiable(existing, pack);

            // when
            trainingService.modifyTrainingPuzzle(10L, new ModifyTrainingPuzzleRequest(
                    1L, null, existing.getBoardStatus(), existing.getAnswer(), 5, "BLACK"));

            // then
            verify(puzzleCacheService, never()).seedSolutionPath(any(), any(), any(), any());
        }

        @Test
        void modifyTrainingPuzzle_WhenAnswerChanges_ThenReseedsTheCache() {
            // given
            Pack pack = pack(1L, "MIDDLE");
            TrainingPuzzle existing = trainingPuzzle(10L, pack, 5, 1234.5, 12);
            givenModifiable(existing, pack);

            // when
            trainingService.modifyTrainingPuzzle(10L, new ModifyTrainingPuzzleRequest(
                    1L, null, existing.getBoardStatus(), "b3", 5, "BLACK"));

            // then
            verify(puzzleCacheService).seedSolutionPath(PuzzleType.TRAINING, 10L, existing.getBoardStatus(), "b3");
        }

        @Test
        void modifyTrainingPuzzle_WhenMovedToAnotherPack_ThenRatesWithThatPackDifficulty() {
            // given
            Pack from = pack(1L, "MIDDLE");
            Pack to = pack(2L, "LOW");
            TrainingPuzzle existing = trainingPuzzle(10L, from, 7, 1234.5, 12);
            givenModifiable(existing, to);

            // when
            TrainingPuzzle modified = trainingService.modifyTrainingPuzzle(10L, new ModifyTrainingPuzzleRequest(
                    2L, null, existing.getBoardStatus(), existing.getAnswer(), 7, "BLACK"));

            // then
            assertThat(modified.getRating())
                    .isEqualTo(RatingUtil.puzzleRating(7, WinColor.getWinColor("BLACK"), to.getDifficulty()));
            assertThat(modified.getRankAttemptCount()).isZero();
        }

        @Test
        void createTrainingPuzzle_WhenCreated_ThenStoresTheAnswerKey() {
            // given
            Pack pack = pack(1L, "LOW");
            when(packRepository.findById(1L)).thenReturn(Optional.of(pack));
            when(trainingPuzzleRepository.save(any(TrainingPuzzle.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            TrainingPuzzle created = trainingService.createTrainingPuzzle(
                    new AddTrainingPuzzleRequest(1L, 0, "a1a2", "a3", 3, "BLACK"));

            // then
            assertThat(created.getAnswerKey()).isEqualTo(BoardUtils.makeAnswerKey("a1a2", "a3"));
        }

        @Test
        void modifyTrainingPuzzle_WhenAnswerChanges_ThenRecomputesTheAnswerKey() {
            // given
            Pack pack = pack(1L, "MIDDLE");
            TrainingPuzzle existing = trainingPuzzle(10L, pack, 5, 1234.5, 12);
            givenModifiable(existing, pack);

            // when
            TrainingPuzzle modified = trainingService.modifyTrainingPuzzle(10L, new ModifyTrainingPuzzleRequest(
                    1L, null, existing.getBoardStatus(), "b3", 5, "BLACK"));

            // then
            assertThat(modified.getAnswerKey()).isEqualTo(BoardUtils.makeAnswerKey(existing.getBoardStatus(), "b3"));
        }

        @Test
        void updatePack_WhenDifficultyChanges_ThenReratesOnlyPuzzlesWithoutRankResults() {
            // given
            Pack existingPack = pack(1L, "LOW");
            TrainingPuzzle unranked = trainingPuzzle(10L, existingPack, 7, 800, 0);
            TrainingPuzzle ranked = trainingPuzzle(11L, existingPack, 7, 1555, 3);

            when(packRepository.findById(1L)).thenReturn(Optional.of(existingPack));
            when(packRepository.save(any(Pack.class))).thenAnswer(inv -> inv.getArgument(0));
            when(packTranslationRepository.findAllByPack_Id(1L)).thenReturn(Collections.emptyList());
            when(trainingPuzzleRepository.findByPack_IdOrderByTrainingIndex(1L)).thenReturn(List.of(unranked, ranked));

            // when
            trainingService.updatePack(1L, new UpdateTrainingPackRequest(Collections.emptyList(), 0, "HIGH"));

            // then
            ArgumentCaptor<List<TrainingPuzzle>> rerated = ArgumentCaptor.captor();
            verify(trainingPuzzleRepository).saveAll(rerated.capture());
            assertThat(rerated.getValue()).singleElement().satisfies(puzzle -> {
                assertThat(puzzle.getId()).isEqualTo(10L);
                assertThat(puzzle.getRating()).isEqualTo(
                        RatingUtil.puzzleRating(7, WinColor.getWinColor("BLACK"), Difficulty.getDifficulty("HIGH")));
            });
        }

        @Test
        void updatePack_WhenDifficultyUnchanged_ThenLeavesPuzzleRatingsAlone() {
            // given
            Pack existingPack = pack(1L, "LOW");
            when(packRepository.findById(1L)).thenReturn(Optional.of(existingPack));
            when(packRepository.save(any(Pack.class))).thenAnswer(inv -> inv.getArgument(0));
            when(packTranslationRepository.findAllByPack_Id(1L)).thenReturn(Collections.emptyList());

            // when
            trainingService.updatePack(1L, new UpdateTrainingPackRequest(Collections.emptyList(), 500, "LOW"));

            // then
            verify(trainingPuzzleRepository, never()).findByPack_IdOrderByTrainingIndex(anyLong());
        }

        @Test
        void recalculateUnrankedPuzzleRatings_WhenSomeRatingsAreStale_ThenSavesOnlyThoseAndCountsThem() {
            // given
            Pack pack = pack(1L, "MIDDLE");
            TrainingPuzzle stale = trainingPuzzle(10L, pack, 7, 1300, 0);
            TrainingPuzzle current = trainingPuzzle(11L, pack, 7, 1000, 0);
            when(trainingPuzzleRepository.findByRankAttemptCount(0)).thenReturn(List.of(stale, current));

            // when
            int changed = trainingService.recalculateUnrankedPuzzleRatings();

            // then
            assertThat(changed).isEqualTo(1);
            ArgumentCaptor<List<TrainingPuzzle>> saved = ArgumentCaptor.captor();
            verify(trainingPuzzleRepository).saveAll(saved.capture());
            assertThat(saved.getValue()).singleElement().satisfies(puzzle -> {
                assertThat(puzzle.getId()).isEqualTo(10L);
                assertThat(puzzle.getRating()).isEqualTo(1000.0);
            });
        }

        private Pack pack(Long id, String difficulty) {
            return Pack.builder()
                    .id(id)
                    .price(0)
                    .puzzleCount(1)
                    .difficulty(Difficulty.getDifficulty(difficulty))
                    .build();
        }

        private TrainingPuzzle trainingPuzzle(Long id, Pack pack, int depth, double rating, int rankAttemptCount) {
            return TrainingPuzzle.builder()
                    .id(id)
                    .pack(pack)
                    .trainingIndex(0)
                    .boardStatus("a1a2")
                    .boardKey("key")
                    .answer("a3")
                    .depth(depth)
                    .rating(rating)
                    .rankAttemptCount(rankAttemptCount)
                    .winColor(WinColor.getWinColor("BLACK"))
                    .build();
        }

        private void givenModifiable(TrainingPuzzle existing, Pack targetPack) {
            when(trainingPuzzleRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
            when(packRepository.findById(targetPack.getId())).thenReturn(Optional.of(targetPack));
            when(trainingPuzzleRepository.save(any(TrainingPuzzle.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        void deleteTrainingPuzzle_WhenPuzzleExists_ThenDeletesAndDecrementsIndexes() {
            // given
            Long puzzleId = 1L;
            int trainingIndex = 5;
            Long packId = 10L;
            Long userId = 1L;

            Pack pack = Pack.builder()
                    .id(packId)
                    .puzzleCount(3)
                    .price(1000)
                    .difficulty(Difficulty.getDifficulty("LOW"))
                    .build();

            TrainingPuzzle puzzle = TrainingPuzzle.builder()
                    .id(puzzleId)
                    .trainingIndex(trainingIndex)
                    .pack(pack)
                    .build();

            UserEntity user = TestUserEntityBuilder.builder()
                    .withId(1L)
                    .save(userRepository);

            SolvedTrainingPuzzle solved = SolvedTrainingPuzzle.builder()
                    .id(999L)
                    .user(user)
                    .puzzle(puzzle)
                    .solvedAt(FIXED_INSTANT)
                    .build();

            when(trainingPuzzleRepository.findById(puzzleId)).thenReturn(Optional.of(puzzle));
            when(solvedTrainingPuzzleRepository.findAllByPuzzleId(puzzleId)).thenReturn(List.of(solved));

            // when
            trainingService.deleteTrainingPuzzle(puzzleId);

            // then
            verify(trainingPuzzleRepository, times(1)).findById(puzzleId);
            verify(trainingPuzzleRepository, times(1)).deleteById(puzzleId);
            verify(trainingPuzzleRepository, times(1)).decreaseIndexesFrom(packId, trainingIndex);
            verify(packRepository, times(1)).decreasePuzzleCount(packId);
            verify(userPackRepository, times(1)).decreaseSolvedCount(userId, packId);
        }

        @Test
        void solveLessonPuzzle_WhenFirstSolve_ThenSavesSolvedTrainingPuzzle() {
            // given
            Long puzzleId = 1L;
            Long userId = 100L;
            Long packId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(userId)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))  // deletedAt set to a future point in time (example)
                    .status(Status.getDefaultStatus())
                    .build();

            Pack pack = Pack.builder()
                    .id(packId)
                    .difficulty(Difficulty.getDifficulty("LOW"))
                    .price(0)
                    .puzzleCount(10)
                    .build();

            TrainingPuzzle trainingPuzzle = TrainingPuzzle.builder()
                    .id(puzzleId)
                    .pack(pack)
                    .build();

            // Assume there is no existing solve record
            when(solvedTrainingPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzleId))
                    .thenReturn(Optional.empty());

            // Puzzle lookup succeeds
            when(trainingPuzzleRepository.findById(puzzleId))
                    .thenReturn(Optional.of(trainingPuzzle));

            // User lookup succeeds (returns a locked, managed entity)
            when(userRepository.findByIdForUpdate(userId))
                    .thenReturn(Optional.of(user));
            when(userPackRepository.existsByUserIdAndPackId(userId, packId)).thenReturn(true);

            // Set up a dummy save result
            when(solvedTrainingPuzzleRepository.save(any(SolvedTrainingPuzzle.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            SolveTrainingPuzzleResponse response = trainingService.solveTrainingPuzzle(user, puzzleId, true);

            // then
            verify(solvedTrainingPuzzleRepository).save(any(SolvedTrainingPuzzle.class));
            verify(userPackRepository).increaseSolvedCount(userId, packId);

            assertThat(response.reward()).isEqualTo(ItemPrice.TRAINING_REWARD.getDefaultPrice());
        }

        @Test
        void solveLessonPuzzle_WhenAlreadySolved_ThenUpdatesSolvedAt() {
            // given
            Long puzzleId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))  // deletedAt set to a future point in time (example)
                    .status(Status.getDefaultStatus())
                    .build();

            Pack pack = Pack.builder().id(1L).build();
            TrainingPuzzle puzzle = TrainingPuzzle.builder().id(puzzleId).pack(pack).build();

            SolvedTrainingPuzzle existingSolvedPuzzle = mock(SolvedTrainingPuzzle.class);
            when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
            when(trainingPuzzleRepository.findById(puzzleId)).thenReturn(Optional.of(puzzle));
            when(userPackRepository.existsByUserIdAndPackId(user.getId(), pack.getId())).thenReturn(true);
            when(solvedTrainingPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzleId))
                    .thenReturn(Optional.of(existingSolvedPuzzle));

            // when
            SolveTrainingPuzzleResponse response = trainingService.solveTrainingPuzzle(user, puzzleId, true);

            // then
            assertThat(response.reward()).isZero();
            verify(existingSolvedPuzzle, times(1)).updateSolvedAtToNow(clock);
            verify(solvedTrainingPuzzleRepository, never()).save(any());
            verify(userPackRepository, never()).increaseSolvedCount(anyLong(), anyLong());
        }

        @Test
        void getTrainingPuzzleList_WhenPackExists_ThenReturnsPuzzlesWithSolvedFlag() {
            // given
            Long packId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))  // deletedAt set to a future point in time (example)
                    .status(Status.getDefaultStatus())
                    .build();

            // Create an example TrainingPuzzle (assume WinColor is already persisted; only use its name)
            WinColor winColor = WinColor.getWinColor("WHITE");
            TrainingPuzzle puzzle = TrainingPuzzle.builder()
                    .id(10L)
                    .boardStatus("a1a2a3")
                    .depth(3)
                    .winColor(winColor)
                    .build();
            List<TrainingPuzzle> puzzles = Collections.singletonList(puzzle);
            when(userPackRepository.existsByUserIdAndPackId(user.getId(), packId)).thenReturn(true);
            when(trainingPuzzleRepository.findByPack_IdOrderByTrainingIndex(packId)).thenReturn(puzzles);

            // Assume solvedTrainingPuzzleRepository.existsByUserAndPuzzle(user, puzzle) returns false
            when(solvedTrainingPuzzleRepository.existsByUserAndPuzzle(user, puzzle)).thenReturn(false);

            // when
            List<GetTrainingPuzzleResponse> response = trainingService.getTrainingPuzzleList(user, packId);

            // then
            assertThat(response).isNotEmpty();
            GetTrainingPuzzleResponse res = response.get(0);
            assertThat(res.id()).isEqualTo(puzzle.getId());
            assertThat(res.boardStatus()).isEqualTo(puzzle.getBoardStatus());
            assertThat(res.depth()).isEqualTo(puzzle.getDepth());
            assertThat(res.winColor()).isEqualTo(winColor.getName());
            assertThat(res.isSolved()).isFalse();
        }

        @Test
        void getTrainingPackList_WhenValidRequest_ThenReturnsPackResponses() {
            // given
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))  // deletedAt set to a future point in time (example)
                    .status(Status.getDefaultStatus())
                    .build();

            GetTrainingPackRequest request = new GetTrainingPackRequest(Difficulty.getDifficulty("LOW").getName(), "EN"); // default lang EN

            // Create Pack (e.g., ID 1, price 1000, puzzleCount 10)
            Pack pack = Pack.builder()
                    .id(1L)
                    .price(1000)
                    .puzzleCount(10)
                    .build();
            List<Pack> packs = Collections.singletonList(pack);
            when(packRepository.findByDifficulty(any(Difficulty.class)))
                    .thenReturn(packs);

            // Create PackTranslation (linked pack with ID 1)
            PackTranslation translation = PackTranslation.builder()
                    .pack(pack)
                    .langCode(LangCode.getLangCode("EN"))
                    .title("Title")
                    .author("Author")
                    .description("Description")
                    .build();

            when(packTranslationRepository.findAllByPack_IdInAndLangCode(
                    eq(List.of(1L)),
                    argThat(arg -> arg.getName().equals("EN"))
            )).thenReturn(List.of(translation));

            // No user pack record: locked, solvedCount 0
            when(userPackRepository.findAllByUserIdAndPackIdIn(100L, List.of(1L)))
                    .thenReturn(Collections.emptyList());

            // when
            List<GetPackResponse> responses = trainingService.getTrainingPackList(user, request);

            // then
            assertThat(responses).hasSize(1);
            GetPackResponse resp = responses.get(0);
            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.title()).isEqualTo("Title");
            assertThat(resp.author()).isEqualTo("Author");
            assertThat(resp.description()).isEqualTo("Description");
            assertThat(resp.price()).isEqualTo(1000);
            assertThat(resp.solvedPuzzleCount()).isZero();
            assertThat(resp.locked()).isTrue();
        }

        @Test
        void purchaseTrainingPack_WhenEnoughCurrency_ThenReturnsPackPrice() {
            // given
            // user's initial balance 2000
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .currency(2000)
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))  // deletedAt set to a future point in time (example)
                    .status(Status.getDefaultStatus())
                    .build();

            Pack pack = Pack.builder()
                    .id(1L)
                    .price(1000)
                    .build();

            PurchaseTrainingPackRequest request = new PurchaseTrainingPackRequest(1L);

            when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
            when(packRepository.findById(1L)).thenReturn(Optional.of(pack));

            // when
            GetPackPurchaseResponse getPackPurchaseResponse = trainingService.purchaseTrainingPack(user, request);

            // then
            assertThat(getPackPurchaseResponse.price()).isEqualTo(1000);
            assertThat(user.getCurrency()).isEqualTo(1000);
            verify(packRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).findByIdForUpdate(user.getId());
            verify(userPackRepository, times(1)).save(any());
        }

        @Test
        void purchaseTrainingPuzzleAnswer_WhenValidPurchase_ThenReturnsAnswerAndPrice() {
            // given
            Pack pack = Pack.builder()
                    .id(1L)
                    .puzzleCount(10)
                    .price(1000)
                    .difficulty(Difficulty.getDifficulty("LOW"))
                    .build();

            Long puzzleId = 1L;
            int hintPrice = ItemPrice.HINT.getDefaultPrice();
            TrainingPuzzle puzzle = TrainingPuzzle.builder()
                    .id(puzzleId)
                    .answer("Correct Answer")
                    .pack(pack)
                    .build();
            when(trainingPuzzleRepository.findById(puzzleId))
                    .thenReturn(Optional.of(puzzle));

            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .currency(500)
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))
                    .status(Status.getDefaultStatus())
                    .build();


            when(userRepository.findByIdForUpdate(user.getId()))
                    .thenReturn(Optional.of(user));
            when(userPackRepository.existsByUserIdAndPackId(user.getId(), pack.getId())).thenReturn(true);

            // when
            GetTrainingPuzzleAnswerResponse response = trainingService.purchaseTrainingPuzzleAnswer(user, puzzleId);

            // then
            assertThat(response.answer()).isEqualTo("Correct Answer");
            assertThat(response.price()).isEqualTo(hintPrice);
        }
    }

    @Nested
    class Failure {
        @Test
        void addTranslation_WhenLanguageAlreadyTranslated_ThenThrowsAlreadyExistingTranslation() {
            // given
            Long packId = 1L;
            Pack pack = Pack.builder().id(packId).build();

            TranslationRequest request = new TranslationRequest(
                    packId,
                    "EN",
                    "For Beginner 1",
                    "Kang Sang-Min",
                    "First time to solve..."
            );

            when(packRepository.findById(packId)).thenReturn(Optional.of(pack));
            when(packTranslationRepository.existsByPackAndLangCode(eq(pack), argThat(arg -> arg.getName().equals("EN"))))
                    .thenReturn(true);

            // when
            CustomException exception = assertThrows(CustomException.class, () -> trainingService.addTranslation(request));

            // then
            assertEquals(ErrorCode.ALREADY_EXISTING_TRANSLATION, exception.getErrorCode());
            verify(packTranslationRepository, never()).save(any(PackTranslation.class));
        }

        @Test
        void addTranslation_WhenPackMissing_ThenThrowsNoSuchTrainingPackWithoutSaving() {
            // given
            Long packId = 1L;
            TranslationRequest request = new TranslationRequest(
                    packId,
                    "EN",
                    "For Beginner 1",
                    "tintin",
                    "First time to solve..."
            );
            when(packRepository.findById(packId)).thenReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class, () -> trainingService.addTranslation(request));

            // then
            assertEquals(ErrorCode.NO_SUCH_TRAINING_PACK, exception.getErrorCode());
            verify(packTranslationRepository, never()).save(any(PackTranslation.class));
        }

        @Test
        void createPack_WhenSameLanguageTwice_ThenThrowsWithoutSaving() {
            // given: language codes are matched without case
            List<PackTranslationRequest> translationRequests = Arrays.asList(
                    new PackTranslationRequest("KO", "초보용 1", "강상민", "설명"),
                    new PackTranslationRequest("ko", "초보용 2", "강상민", "설명")
            );
            CreateTrainingPackRequest request = new CreateTrainingPackRequest(translationRequests, 1000, "LOW");

            // when
            CustomException exception = assertThrows(CustomException.class, () -> trainingService.createPack(request));

            // then
            assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
            verify(packRepository, never()).save(any(Pack.class));
            verify(packTranslationRepository, never()).saveAll(anyList());
        }

        @Test
        void updatePack_WhenSameLanguageTwice_ThenThrowsWithoutChangingThePack() {
            // given
            List<PackTranslationRequest> translationRequests = Arrays.asList(
                    new PackTranslationRequest("EN", "Beginner 1", "Kang", "Description"),
                    new PackTranslationRequest("EN", "Beginner 2", "Kang", "Description")
            );
            UpdateTrainingPackRequest request = new UpdateTrainingPackRequest(translationRequests, 1200, "HIGH");

            // when
            CustomException exception = assertThrows(CustomException.class, () -> trainingService.updatePack(1L, request));

            // then
            assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
            verify(packRepository, never()).save(any(Pack.class));
            verify(packTranslationRepository, never()).deleteAll(anyList());
            verify(packTranslationRepository, never()).saveAll(anyList());
        }

        @Test
        void updatePack_WhenPackMissing_ThenThrowsNoSuchTrainingPackWithoutSaving() {
            // given
            Long packId = 1L;
            List<PackTranslationRequest> translationRequests = Collections.singletonList(
                    new PackTranslationRequest("KO", "초보용 1(수정)", "강상민", "설명(수정)")
            );
            UpdateTrainingPackRequest request = new UpdateTrainingPackRequest(translationRequests, 1200, "HIGH");

            when(packRepository.findById(packId)).thenReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class, () -> trainingService.updatePack(packId, request));

            // then
            assertEquals(ErrorCode.NO_SUCH_TRAINING_PACK, exception.getErrorCode());
            verify(packRepository, never()).save(any(Pack.class));
            verify(packTranslationRepository, never()).findAllByPack_Id(anyLong());
            verify(packTranslationRepository, never()).deleteAll(anyList());
            verify(packTranslationRepository, never()).saveAll(anyList());
        }

        @Test
        void createTrainingPuzzle_WhenPackMissing_ThenThrowsNoSuchTrainingPack() {
            // given
            Long packId = 1L;
            String boardStatus = "a1a2a3a4";
            Integer depth = 3;
            String winColorStr = "WHITE";

            AddTrainingPuzzleRequest request = new AddTrainingPuzzleRequest(
                    packId,
                    null,
                    boardStatus,
                    "answer",
                    depth,
                    winColorStr
            );

            // Case where the Pack does not exist
            when(packRepository.findById(packId)).thenReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class,
                    () -> trainingService.createTrainingPuzzle(request));

            // then
            assertEquals(ErrorCode.NO_SUCH_TRAINING_PACK, exception.getErrorCode());

            // Since there is no Pack, save() should not be called
            verify(trainingPuzzleRepository, never()).save(any(TrainingPuzzle.class));
        }

        @Test
        void deleteTrainingPuzzle_WhenPuzzleMissing_ThenThrowsCannotFindTrainingPuzzle() {
            // given
            Long puzzleId = 1L;
            when(trainingPuzzleRepository.findById(puzzleId)).thenReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class, () -> trainingService.deleteTrainingPuzzle(puzzleId));

            // then
            assertEquals(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE, exception.getErrorCode());

            verify(trainingPuzzleRepository, times(1)).findById(puzzleId);
            verify(trainingPuzzleRepository, never()).deleteById(anyLong());
            verify(trainingPuzzleRepository, never()).decreaseIndexesFrom(anyLong(), anyInt());
        }



        @Test
        void solveLessonPuzzle_WhenPuzzleMissing_ThenThrowsCannotFindTrainingPuzzle() {
            // given
            Long puzzleId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))  // deletedAt set to a future point in time (example)
                    .status(Status.getDefaultStatus())
                    .build();

            // The puzzle does not exist
            when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
            when(trainingPuzzleRepository.findById(puzzleId))
                    .thenReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class,
                    () -> trainingService.solveTrainingPuzzle(user, puzzleId, true));

            // then
            assertEquals(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE, exception.getErrorCode());

            verify(trainingPuzzleRepository, times(1))
                    .findById(puzzleId);
            verify(solvedTrainingPuzzleRepository, never()).save(any(SolvedTrainingPuzzle.class));
        }

        @Test
        void solveTrainingPuzzle_WhenPackNotOwned_ThenThrowsWithoutReward() {
            // given
            Pack pack = Pack.builder().id(5L).build();
            TrainingPuzzle puzzle = TrainingPuzzle.builder().id(1L).pack(pack).build();
            UserEntity user = TestUserEntityBuilder.builder().withId(100L).withCurrency(0).build();

            when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
            when(trainingPuzzleRepository.findById(puzzle.getId())).thenReturn(Optional.of(puzzle));
            when(userPackRepository.existsByUserIdAndPackId(user.getId(), pack.getId())).thenReturn(false);

            // when
            CustomException exception = assertThrows(CustomException.class,
                    () -> trainingService.solveTrainingPuzzle(user, puzzle.getId(), true));

            // then
            assertEquals(ErrorCode.PACK_NOT_OWNED, exception.getErrorCode());
            assertThat(user.getCurrency()).isZero();
            verify(solvedTrainingPuzzleRepository, never()).save(any(SolvedTrainingPuzzle.class));
        }

        @Test
        void purchaseTrainingPuzzleAnswer_WhenPackNotOwned_ThenThrowsWithoutCharging() {
            // given
            Pack pack = Pack.builder().id(5L).build();
            TrainingPuzzle puzzle = TrainingPuzzle.builder().id(1L).answer("h11").pack(pack).build();
            UserEntity user = TestUserEntityBuilder.builder().withId(100L).withCurrency(1000).build();

            when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
            when(trainingPuzzleRepository.findById(puzzle.getId())).thenReturn(Optional.of(puzzle));
            when(userPackRepository.existsByUserIdAndPackId(user.getId(), pack.getId())).thenReturn(false);

            // when
            CustomException exception = assertThrows(CustomException.class,
                    () -> trainingService.purchaseTrainingPuzzleAnswer(user, puzzle.getId()));

            // then
            assertEquals(ErrorCode.PACK_NOT_OWNED, exception.getErrorCode());
            assertThat(user.getCurrency()).isEqualTo(1000);
        }

        @Test
        void getTrainingPuzzleList_WhenPackNotOwned_ThenThrowsWithoutListing() {
            // given
            UserEntity user = TestUserEntityBuilder.builder().withId(100L).build();
            when(userPackRepository.existsByUserIdAndPackId(user.getId(), 5L)).thenReturn(false);

            // when
            CustomException exception = assertThrows(CustomException.class,
                    () -> trainingService.getTrainingPuzzleList(user, 5L));

            // then
            assertEquals(ErrorCode.PACK_NOT_OWNED, exception.getErrorCode());
            verify(trainingPuzzleRepository, never()).findByPack_IdOrderByTrainingIndex(anyLong());
        }

        @Test
        void deletePack_WhenStarterPack_ThenThrowsAndDeletesNothing() {
            // when
            CustomException exception = assertThrows(CustomException.class,
                    () -> trainingService.deletePack(Pack.STARTER_PACK_ID));

            // then
            assertEquals(ErrorCode.CANNOT_DELETE_STARTER_PACK, exception.getErrorCode());
            verify(packRepository, never()).delete(any());
            verify(userPackRepository, never()).deleteAllByPack_Id(anyLong());
        }

        @Test
        void getTrainingPuzzleList_WhenPackMissing_ThenThrowsNoSuchTrainingPack() {
            // given
            Long packId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))  // deletedAt set to a future point in time (example)
                    .status(Status.getDefaultStatus())
                    .build();

            when(userPackRepository.existsByUserIdAndPackId(user.getId(), packId)).thenReturn(true);
            when(trainingPuzzleRepository.findByPack_IdOrderByTrainingIndex(packId)).thenReturn(Collections.emptyList());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    trainingService.getTrainingPuzzleList(user, packId)
            );
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_SUCH_TRAINING_PACK);

            // Verify there are no unnecessary additional calls (if desired)
            verifyNoMoreInteractions(trainingPuzzleRepository, solvedTrainingPuzzleRepository);
        }

        @Test
        void getTrainingPackList_WhenNoPacksExist_ThenThrowsNoSuchTrainingPacks() {
            // given
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))  // deletedAt set to a future point in time (example)
                    .status(Status.getDefaultStatus())
                    .build();
            GetTrainingPackRequest request = new GetTrainingPackRequest("LOW", null);

            when(packRepository.findByDifficulty(any(Difficulty.class)))
                    .thenReturn(Collections.emptyList());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    trainingService.getTrainingPackList(user, request)
            );
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_SUCH_TRAINING_PACKS);
        }

        @Test
        void purchaseTrainingPack_WhenNotEnoughCurrency_ThenThrowsInsufficientCurrency() {
            // given
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .currency(500)
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))  // deletedAt set to a future point in time (example)
                    .status(Status.getDefaultStatus())
                    .build();

            Pack pack = Pack.builder()
                    .id(1L)
                    .price(1000)
                    .build();
            PurchaseTrainingPackRequest request = new PurchaseTrainingPackRequest(1L);

            when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
            when(packRepository.findById(1L)).thenReturn(Optional.of(pack));

            // when
            CustomException exception = assertThrows(CustomException.class, () ->
                    trainingService.purchaseTrainingPack(user, request)
            );
            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_CURRENCY);
            verify(packRepository, times(1)).findById(1L);
            verify(userRepository, never()).save(any(UserEntity.class));
            verify(userPackRepository, never()).save(any());
        }

        @Test
        void purchaseTrainingPack_WhenAlreadyOwned_ThenThrowsAlreadyOwnedPackAndDoesNotCharge() {
            // given
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .currency(2000)
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))
                    .status(Status.getDefaultStatus())
                    .build();

            Pack pack = Pack.builder()
                    .id(1L)
                    .price(1000)
                    .build();
            PurchaseTrainingPackRequest request = new PurchaseTrainingPackRequest(1L);

            when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
            when(packRepository.findById(1L)).thenReturn(Optional.of(pack));
            when(userPackRepository.existsByUserIdAndPackId(user.getId(), pack.getId())).thenReturn(true);

            // when
            CustomException exception = assertThrows(CustomException.class, () ->
                    trainingService.purchaseTrainingPack(user, request)
            );

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_OWNED_PACK);
            assertThat(user.getCurrency()).isEqualTo(2000);
            verify(userPackRepository, never()).save(any());
        }

        @Test
        void purchaseTrainingPuzzleAnswer_WhenPuzzleMissing_ThenThrowsCannotFindTrainingPuzzle() {
            // given
            Long puzzleId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .currency(500)
                    .lastAccessedAt(FIXED_INSTANT)
                    .deletedAt(FIXED_INSTANT.plus(1, ChronoUnit.DAYS))
                    .status(Status.getDefaultStatus())
                    .build();

            when(trainingPuzzleRepository.findById(puzzleId))
                    .thenReturn(Optional.empty());

            when(userRepository.findByIdForUpdate(user.getId()))
                    .thenReturn(Optional.of(user));

            // when
            CustomException exception = assertThrows(CustomException.class, () ->
                    trainingService.purchaseTrainingPuzzleAnswer(user, puzzleId)
            );

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE);
        }

    }

}
