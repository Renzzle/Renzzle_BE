package com.renzzle.backend.domain.puzzle.training.service;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.cache.dao.PuzzleCacheRepository;
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
import org.junit.jupiter.api.DisplayName;
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
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private TrainingService trainingService;

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @DisplayName("createPack : 팩 생성")
        @Test
        void testCreatePack() {
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

        @DisplayName("updatePack : 팩 수정 (번역/가격/난이도)")
        @Test
        void testUpdatePack() {
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

        @DisplayName("addTranslation : Pack에 번역을 추가")
        @Test
        void testAddTranslation() {
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

        @DisplayName("createTrainingPuzzle: 유효한 요청과 존재하는 Pack을 전달하면 TrainingPuzzle이 정상적으로 생성")
        @Test
        void testCreateTrainingPuzzle() {
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

            // The service logic builds a TrainingPuzzle entity and then calls trainingPuzzleRepository.save()
            // The unsaved Puzzle is constructed internally; mock it to return the savedPuzzle with an assigned id.
            TrainingPuzzle savedPuzzle = TrainingPuzzle.builder()
                    .id(100L)
                    .pack(pack)
                    .trainingIndex(6)
                    .answer("a1a2a3a4")
                    .boardStatus(boardStatus)
                    .boardKey("generatedKey")
                    .depth(depth)
                    .rating(depth * 200) // TODO: manage the rating calculation formula via RatingUtils
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
            assertEquals(600.0, result.getRating());

            verify(trainingPuzzleRepository, times(1)).findTopIndex(packId);
            verify(packRepository, times(1)).findById(packId);
            verify(packRepository, times(1)).increasePuzzleCount(packId);
            verify(trainingPuzzleRepository, times(1)).save(any(TrainingPuzzle.class));
        }

        @DisplayName("testDeleteTrainingPuzzle: 존재하는 퍼즐 id가 주어지면 퍼즐 삭제 및 인덱스 감소")
        @Test
        void testDeleteTrainingPuzzle() {
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
            verify(trainingPuzzleRepository, times(1)).decreaseIndexesFrom(trainingIndex);
            verify(packRepository, times(1)).decreasePuzzleCount(packId);
            verify(userPackRepository, times(1)).decreaseSolvedCount(userId, packId);
        }

        @DisplayName("SolveLessonPuzzle: 주어진 user와 puzzleId에 대해 최초 풀이라면 solvedTrainingPuzzle이 저장")
        @Test
        void testSolveLessonPuzzle() {
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

            // User lookup succeeds (returns a managed entity)
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));

            // Set up a dummy save result
            when(solvedTrainingPuzzleRepository.save(any(SolvedTrainingPuzzle.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            SolveTrainingPuzzleResponse response = trainingService.solveTrainingPuzzle(user, puzzleId, true);

            // then
            verify(solvedTrainingPuzzleRepository).save(any(SolvedTrainingPuzzle.class));
            verify(userPackRepository).increaseSolvedCount(userId, packId);

            assertThat(response.reward()).isEqualTo(20); // ItemPrice.TRAINING_LOW_REWARD.getPrice()
        }

        @DisplayName("testSolveLessonPuzzle_AlreadySolved: 이미 풀이한 퍼즐의 경우 solvedAt을 갱신")
        @Test
        void testSolveLessonPuzzle_AlreadySolved() {
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

            SolvedTrainingPuzzle existingSolvedPuzzle = mock(SolvedTrainingPuzzle.class);
            when(solvedTrainingPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzleId))
                    .thenReturn(Optional.of(existingSolvedPuzzle));

            // when
            SolveTrainingPuzzleResponse response = trainingService.solveTrainingPuzzle(user, puzzleId, true);

            // then
            assertThat(response.reward()).isZero();
            verify(existingSolvedPuzzle, times(1)).updateSolvedAtToNow(clock);
            verify(solvedTrainingPuzzleRepository, never()).save(any());
            verify(trainingPuzzleRepository, never()).findById(any());
        }

        @Test
        @DisplayName("getTrainingPuzzleList : 유효한 Pack ID에 대해 TrainingPuzzle 목록과 solved 여부가 올바르게 조회")
        void testGetTrainingPuzzleList() {
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
        @DisplayName("testGetTrainingPackList: 유효한 요청 시, 올바른 GetPackResponse 리스트 반환")
        void testGetTrainingPackList() {
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

            // userPackRepository: the user has no record for this pack, so return an empty list (locked = true, solvedCount = 0)
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
        @DisplayName("testPurchaseTrainingPack: 충분한 잔액을 가진 사용자가 팩 구매 시, 팩 금액 반환")
        void testPurchaseTrainingPack() {
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

            when(packRepository.findById(1L)).thenReturn(Optional.of(pack));

            // Meaning: whatever UserEntity is requested to be saved, return that UserEntity object itself
            when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            GetPackPurchaseResponse getPackPurchaseResponse = trainingService.purchaseTrainingPack(user, request);

            // then
            assertThat(getPackPurchaseResponse.price()).isEqualTo(1000);
            verify(packRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).save(user);
            verify(userPackRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("testPurchaseTrainingPuzzleAnswer: 정상 구매 시, 퍼즐 정답과 퍼즐 정답보기 금액 반환")
        void testPurchaseTrainingPuzzleAnswer() {
            // given
            Pack pack = Pack.builder()
                    .id(1L)
                    .puzzleCount(10)
                    .price(1000)
                    .difficulty(Difficulty.getDifficulty("LOW"))
                    .build();

            Long puzzleId = 1L;
            int hintPrice = ItemPrice.HINT.getPrice();
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


            when(userRepository.findById(user.getId()))
                    .thenReturn(Optional.of(user));

            // when
            GetTrainingPuzzleAnswerResponse response = trainingService.purchaseTrainingPuzzleAnswer(user, puzzleId);

            // then
            assertThat(response.answer()).isEqualTo("Correct Answer");
            assertThat(response.price()).isEqualTo(hintPrice);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @DisplayName("addTranslation : Pack에 이미 해당 언어의 번역이 존재하기에 ALREADY_EXISTING_TRANSLATION 예외 처리")
        @Test
        void testAddTranslation_AlreadyExists() {
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

        @DisplayName("addTranslation: Pack이 존재하지 않으면 번역 저장을 시도하지 않고 NO_SUCH_TRAINING_PACK 예외를 던진다")
        @Test
        void testAddTranslation_PackNotFound() {
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

        @DisplayName("updatePack: Pack이 존재하지 않으면 번역/팩 저장을 시도하지 않고 NO_SUCH_TRAINING_PACK 예외를 던진다")
        @Test
        void testUpdatePack_PackNotFound() {
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

        @DisplayName("Pack이 존재하지 않는 경우 NO_SUCH_TRAINING_PACK 예외처리")
        @Test
        void testCreateTrainingPuzzle_PackNotFound() {
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

        @DisplayName("DeleteTrainingPuzzle: 존재하지 않는 퍼즐 id가 주어지면 CANNOT_FIND_TRAINING_PUZZLE 예외 처리")
        @Test
        void testDeleteTrainingPuzzle_PuzzleNotFound() {
            // given
            Long puzzleId = 1L;
            when(trainingPuzzleRepository.findById(puzzleId)).thenReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class, () -> trainingService.deleteTrainingPuzzle(puzzleId));

            // then
            assertEquals(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE, exception.getErrorCode());

            verify(trainingPuzzleRepository, times(1)).findById(puzzleId);
            verify(trainingPuzzleRepository, never()).deleteById(anyLong());
            verify(trainingPuzzleRepository, never()).decreaseIndexesFrom(anyInt());
        }



        @DisplayName("testSolveLessonPuzzle_PuzzleNotFound: 주어진 puzzleId에 해당하는 퍼즐이 없으면 CANNOT_FIND_TRAINING_PUZZLE 예외가 발생")
        @Test
        void testSolveLessonPuzzle_PuzzleNotFound() {
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

            // No existing solve record, and the puzzle does not exist
            when(solvedTrainingPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzleId))
                    .thenReturn(Optional.empty());
            when(trainingPuzzleRepository.findById(puzzleId))
                    .thenReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class,
                    () -> trainingService.solveTrainingPuzzle(user, puzzleId, true));

            // then
            assertEquals(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE, exception.getErrorCode());

            verify(solvedTrainingPuzzleRepository, times(1))
                    .findByUserIdAndPuzzleId(user.getId(), puzzleId);
            verify(trainingPuzzleRepository, times(1))
                    .findById(puzzleId);
            verify(solvedTrainingPuzzleRepository, never()).save(any(SolvedTrainingPuzzle.class));
        }

        @Test
        @DisplayName("testGetTrainingPuzzleList : 존재하지 않는 Pack ID에 대해 TrainingPuzzle 목록이 없으면 NO_SUCH_TRAINING_PACK 예외를 던진다.")
        void testGetTrainingPuzzleList_PackNotFound() {
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
        @DisplayName("testGetTrainingPackList: Pack 목록이 비어 있으면 NO_SUCH_TRAINING_PACKS 예외 발생")
        void testGetTrainingPackList_NoTrainingPack() {
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
        @DisplayName("testPurchaseTrainingPack: 잔액이 부족하면 INSUFFICIENT_CURRENCY 예외가 발생한다.")
        void testPurchaseTrainingPack_NotEnoughCurrency() {
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
        @DisplayName("testPurchaseTrainingPuzzleAnswer: 존재하지 않는 퍼즐 ID로 구매 시, CANNOT_FIND_TRAINING_PUZZLE 예외 발생")
        void testPurchaseTrainingPuzzleAnswer_CannotPurchasePuzzle() {
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

            when(userRepository.findById(user.getId()))
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
