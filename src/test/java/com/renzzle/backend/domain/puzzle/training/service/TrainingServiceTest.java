package com.renzzle.backend.domain.puzzle.training.service;

import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.training.api.response.GetPackResponse;
import com.renzzle.backend.domain.puzzle.training.api.response.GetTrainingPuzzleAnswerResponse;
import com.renzzle.backend.domain.puzzle.training.api.response.GetTrainingPuzzleResponse;
import com.renzzle.backend.domain.puzzle.training.api.request.*;
import com.renzzle.backend.domain.puzzle.training.dao.*;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import com.renzzle.backend.domain.puzzle.training.domain.PackTranslation;
import com.renzzle.backend.domain.puzzle.training.domain.SolvedTrainingPuzzle;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.puzzle.training.service.TrainingService;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.constant.ItemPrice;
import com.renzzle.backend.global.common.constant.LanguageCode;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        public void testCreatePack() {
            // given
            List<PackTranslationRequest> translationRequests = Arrays.asList(
                    new PackTranslationRequest(LanguageCode.ko, "초보용 1", "강상민", "처음 퍼즐을 푸는..."),
                    new PackTranslationRequest(LanguageCode.en, "For Beginner 1", "Kang Sang-Min", "First time to solve...")
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
                            .languageCode(info.langCode().name())
                            .title(info.title())
                            .author(info.author())
                            .description(info.description())
                            .build())
                    .collect(Collectors.toList());

            when(packTranslationRepository.saveAll(anyList())).thenReturn(expectedTranslations);

            // when
            Pack result = trainingService.createPack(request);

            // then
            assertNotNull(result);
            assertEquals(savedPack.getId(), result.getId());
            verify(packRepository, times(1)).save(any(Pack.class));
            verify(packTranslationRepository, times(1)).saveAll(anyList());
        }

        @DisplayName("addTranslation : Pack에 번역을 추가")
        @Test
        public void testAddTranslation() {
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
                    LanguageCode.en,
                    "For Beginner 1",
                    "Kang Sang-Min",
                    "First time to solve..."
            );

            when(packRepository.findById(packId)).thenReturn(Optional.of(pack));
            when(packTranslationRepository.existsByPackAndLanguageCode(pack, request.langCode().name())).thenReturn(false);

            // when
            trainingService.addTranslation(request);

            // ArgumentCaptor 를 통해 addTranslation 되는 PackTranslation 객체를 가져옴
            ArgumentCaptor<PackTranslation> captor = ArgumentCaptor.forClass(PackTranslation.class);

            // then
            verify(packTranslationRepository, times(1)).save(captor.capture());

            PackTranslation savedTranslation = captor.getValue();
            assertEquals(pack, savedTranslation.getPack());
            assertEquals(request.langCode().name(), savedTranslation.getLanguageCode());
            assertEquals(request.title(), savedTranslation.getTitle());
            assertEquals(request.author(), savedTranslation.getAuthor());
            assertEquals(request.description(), savedTranslation.getDescription());
        }

        @DisplayName("createTrainingPuzzle: 유효한 요청과 존재하는 Pack을 전달하면 TrainingPuzzle이 정상적으로 생성")
        @Test
        public void testCreateTrainingPuzzle() {
            // given
            Long packId = 1L;
            String boardStatus = "a1a2a3a4";
            Integer depth = 3;
            WinColor winColorStr = WinColor.getWinColor("WHITE"); // WinColor의 유효한 값이라고 가정

            AddTrainingPuzzleRequest request = new AddTrainingPuzzleRequest(
                    packId,
                    null,
                    boardStatus,
                    "a1a2a3a4",
                    depth,
                    winColorStr
            );

            when(trainingPuzzleRepository.findTopIndex(packId)).thenReturn(5);

            // Pack 객체 생성 (필요한 필드만 채움)
            Pack pack = Pack.builder()
                    .id(packId)
                    .puzzleCount(0)    // 초기 puzzleCount 값 (예시)
                    .price(1000)
                    .difficulty(Difficulty.getDifficulty("LOW"))
                    .build();
            when(packRepository.findById(packId)).thenReturn(Optional.of(pack));

            // increasePuzzleCount 는 void 메소드이므로 doNothing() 처리
            doNothing().when(packRepository).increasePuzzleCount(packId);

            // 서비스 로직에서는 TrainingPuzzle 엔티티를 빌드한 후 trainingPuzzleRepository.save()를 호출
            // unsaved Puzzle은 내부에서 구성되며, 최종적으로 id가 할당된 savedPuzzle을 반환하도록 모킹함.
            TrainingPuzzle savedPuzzle = TrainingPuzzle.builder()
                    .id(100L)
                    .pack(pack)
                    .trainingIndex(6)
                    .boardStatus(boardStatus)
                    .boardKey("generatedKey")
                    .depth(depth)
                    .rating(depth * 200) // TODO: RatingUtils로 레이팅 계산식 관리
                    .winColor(winColorStr)
                    .build();

            when(trainingPuzzleRepository.save(any(TrainingPuzzle.class))).thenReturn(savedPuzzle);

            // when
            TrainingPuzzle result = trainingService.createTrainingPuzzle(request);

            // then
            assertNotNull(result);
            assertEquals(100L, result.getId());

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
        public void testDeleteTrainingPuzzle() {
            // given
            Long puzzleId = 1L;
            int trainingIndex = 5;
            TrainingPuzzle puzzle = TrainingPuzzle.builder()
                    .id(puzzleId)
                    .trainingIndex(trainingIndex)
                    .build();

            when(trainingPuzzleRepository.findById(puzzleId)).thenReturn(Optional.of(puzzle));

            // when
            trainingService.deleteTrainingPuzzle(puzzleId);

            // then
            verify(trainingPuzzleRepository, times(1)).findById(puzzleId);
            verify(trainingPuzzleRepository, times(1)).deleteById(puzzleId);
            verify(trainingPuzzleRepository, times(1)).decreaseIndexesFrom(trainingIndex);
        }

        @DisplayName("SolveLessonPuzzle: 주어진 user와 puzzleId에 대해 최초 풀이라면 solvedTrainingPuzzle이 저장")
        @Test
        public void testSolveLessonPuzzle() {
            // given
            Long puzzleId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(Instant.now())
                    .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))  // deletedAt은 미래 시점으로 설정 (예시)
                    .status(Status.getDefaultStatus())
                    .build();

            TrainingPuzzle trainingPuzzle = TrainingPuzzle.builder()
                    .id(puzzleId)
                    .build();

            // 기존에 풀이 기록이 없음을 가정
            when(solvedTrainingPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzleId))
                    .thenReturn(Optional.empty());
            // 퍼즐이 존재함을 가정
            when(trainingPuzzleRepository.findById(puzzleId))
                    .thenReturn(Optional.of(trainingPuzzle));
            // 저장 시 어떤 객체가 반환되든 상관 없으므로 모킹
            SolvedTrainingPuzzle savedSolvedPuzzle = SolvedTrainingPuzzle.builder()
                    .user(user)
                    .puzzle(trainingPuzzle)
                    .build();
            when(solvedTrainingPuzzleRepository.save(any(SolvedTrainingPuzzle.class)))
                    .thenReturn(savedSolvedPuzzle);

            // when
            trainingService.solveTrainingPuzzle(user, puzzleId);

            // then
            verify(solvedTrainingPuzzleRepository, times(1))
                    .findByUserIdAndPuzzleId(user.getId(), puzzleId);
            verify(trainingPuzzleRepository, times(1))
                    .findById(puzzleId);
            verify(solvedTrainingPuzzleRepository, times(1))
                    .save(any(SolvedTrainingPuzzle.class));
        }

        @DisplayName("testSolveLessonPuzzle_AlreadySolved: 이미 풀이한 퍼즐의 경우 solvedAt을 갱신")
        @Test
        public void testSolveLessonPuzzle_AlreadySolved() {
            // given
            Long puzzleId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(Instant.now())
                    .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))  // deletedAt은 미래 시점으로 설정 (예시)
                    .status(Status.getDefaultStatus())
                    .build();

            SolvedTrainingPuzzle existingSolvedPuzzle = mock(SolvedTrainingPuzzle.class);
            when(solvedTrainingPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzleId))
                    .thenReturn(Optional.of(existingSolvedPuzzle));

            // when
            trainingService.solveTrainingPuzzle(user, puzzleId);

            // then
            verify(existingSolvedPuzzle, times(1)).updateSolvedAtToNow(clock);
            verify(solvedTrainingPuzzleRepository, never()).save(any());
            verify(trainingPuzzleRepository, never()).findById(any());
        }

        @Test
        @DisplayName("getTrainingPuzzleList : 유효한 Pack ID에 대해 TrainingPuzzle 목록과 solved 여부가 올바르게 조회")
        public void testGetTrainingPuzzleList() {
            // given
            Long packId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(Instant.now())
                    .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))  // deletedAt은 미래 시점으로 설정 (예시)
                    .status(Status.getDefaultStatus())
                    .build();

            // 예시 TrainingPuzzle 생성 (WinColor는 미리 영속화된 것으로 가정하고, 이름만 사용)
            WinColor winColor = WinColor.getWinColor("WHITE");
            TrainingPuzzle puzzle = TrainingPuzzle.builder()
                    .id(10L)
                    .boardStatus("a1a2a3")
                    .depth(3)
                    .winColor(winColor)
                    .build();
            List<TrainingPuzzle> puzzles = Collections.singletonList(puzzle);
            when(trainingPuzzleRepository.findByPack_Id(packId)).thenReturn(puzzles);

            // solvedTrainingPuzzleRepository.existsByUserAndPuzzle(user, puzzle) 가 false라고 가정
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
        public void testGetTrainingPackList() {
            // given
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(Instant.now())
                    .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))  // deletedAt은 미래 시점으로 설정 (예시)
                    .status(Status.getDefaultStatus())
                    .build();

            GetTrainingPackRequest request = new GetTrainingPackRequest(Difficulty.getDifficulty("LOW").getName(), null); // lang 기본값 EN

            // Pack 생성 (예: ID 1, price 1000, puzzleCount 10)
            Pack pack = Pack.builder()
                    .id(1L)
                    .price(1000)
                    .puzzleCount(10)
                    .build();
            List<Pack> packs = Collections.singletonList(pack);
            when(packRepository.findByDifficulty(any(Difficulty.class)))
                    .thenReturn(packs);

            // PackTranslation 생성 (연결된 pack의 ID 1)
            PackTranslation translation = PackTranslation.builder()
                    .pack(pack)
                    .languageCode(LanguageCode.en.name())
                    .title("Title")
                    .author("Author")
                    .description("Description")
                    .build();
            when(packTranslationRepository.findAllByPack_IdInAndLanguageCode(List.of(1L), LanguageCode.en.name()))
                    .thenReturn(Collections.singletonList(translation));

            // userPackRepository: 사용자가 해당 pack에 대한 기록이 없으므로, 빈 리스트 반환 (locked = true, solvedCount = 0)
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
            assertThat(resp.solvedPuzzleCount()).isEqualTo(0);
            assertThat(resp.locked()).isTrue();
        }

        @Test
        @DisplayName("testPurchaseTrainingPack: 충분한 잔액을 가진 사용자가 팩 구매 시, 잔액이 차감되어 반환")
        public void testPurchaseTrainingPack() {
            // given
            // 사용자 초기 잔액 2000
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .currency(2000)
                    .lastAccessedAt(Instant.now())
                    .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))  // deletedAt은 미래 시점으로 설정 (예시)
                    .status(Status.getDefaultStatus())
                    .build();

            Pack pack = Pack.builder()
                    .id(1L)
                    .price(1000)
                    .build();

            PurchaseTrainingPackRequest request = new PurchaseTrainingPackRequest(1L);

            when(packRepository.findById(1L)).thenReturn(Optional.of(pack));

            // 어떤 UserEntity가 저장 요청되더라도, 그 UserEntity 객체 자체를 반환하라 라는 의미
            when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Integer remainingCurrency = trainingService.purchaseTrainingPack(user, request);

            // then
            assertThat(remainingCurrency).isEqualTo(1000);
            verify(packRepository, times(1)).findById(1L);
            verify(userRepository, times(1)).save(user);
            verify(userPackRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("testPurchaseTrainingPuzzleAnswer: 정상 구매 시, 퍼즐 정답과 업데이트된 사용자 잔액을 반환")
        public void testPurchaseTrainingPuzzleAnswer() {
            // given
            Long puzzleId = 1L;
            int hintPrice = ItemPrice.HINT.getPrice();
            TrainingPuzzle puzzle = TrainingPuzzle.builder()
                    .id(puzzleId)
                    .answer("Correct Answer")
                    .build();
            when(trainingPuzzleRepository.findById(eq(puzzleId)))
                    .thenReturn(Optional.of(puzzle));

            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .currency(500)
                    .lastAccessedAt(Instant.now())
                    .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .status(Status.getDefaultStatus())
                    .build();


            when(userRepository.findById(eq(user.getId())))
                    .thenReturn(Optional.of(user));

            PurchaseTrainingPuzzleAnswerRequest request = new PurchaseTrainingPuzzleAnswerRequest(puzzleId);

            // when
            GetTrainingPuzzleAnswerResponse response = trainingService.purchaseTrainingPuzzleAnswer(user, request);

            // then
            assertThat(response.answer()).isEqualTo("Correct Answer");
            assertThat(response.currency()).isEqualTo(500 - hintPrice);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @DisplayName("addTranslation : Pack에 이미 해당 언어의 번역이 존재하기에 ALREADY_EXISTING_TRANSLATION 예외 처리")
        @Test
        public void testAddTranslation_AlreadyExists() {
            // given
            Long packId = 1L;
            Pack pack = Pack.builder().id(packId).build();
            TranslationRequest request = new TranslationRequest(
                    packId,
                    LanguageCode.en,
                    "For Beginner 1",
                    "Kang Sang-Min",
                    "First time to solve..."
            );

            when(packRepository.findById(packId)).thenReturn(Optional.of(pack));
            when(packTranslationRepository.existsByPackAndLanguageCode(pack, request.langCode().name())).thenReturn(true);

            // when
            CustomException exception = assertThrows(CustomException.class, () -> trainingService.addTranslation(request));

            // then
            assertEquals(ErrorCode.ALREADY_EXISTING_TRANSLATION, exception.getErrorCode());
            verify(packTranslationRepository, never()).save(any(PackTranslation.class));
        }

        @DisplayName("addTranslation: Pack이 존재하지 않으면 번역 저장을 시도하지 않고 NO_SUCH_TRAINING_PACK 예외를 던진다")
        @Test
        public void testAddTranslation_PackNotFound() {
            // given
            Long packId = 1L;
            TranslationRequest request = new TranslationRequest(
                    packId,
                    LanguageCode.en,
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

        @DisplayName("Pack이 존재하지 않는 경우 NO_SUCH_TRAINING_PACK 예외처리")
        @Test
        public void testCreateTrainingPuzzle_PackNotFound() {
            // given
            Long packId = 1L;
            String boardStatus = "a1a2a3a4";
            Integer depth = 3;
            WinColor winColorStr = WinColor.getWinColor("WHITE");

            AddTrainingPuzzleRequest request = new AddTrainingPuzzleRequest(
                    packId,
                    null,
                    boardStatus,
                    "answer",
                    depth,
                    winColorStr
            );

            // Pack이 존재하지 않는 경우
            when(packRepository.findById(packId)).thenReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class,
                    () -> trainingService.createTrainingPuzzle(request));

            // then
            assertEquals(ErrorCode.NO_SUCH_TRAINING_PACK, exception.getErrorCode());

            // Pack이 없으므로 save()가 호출되지 않아야 함
            verify(trainingPuzzleRepository, never()).save(any(TrainingPuzzle.class));
        }

        @DisplayName("DeleteTrainingPuzzle: 존재하지 않는 퍼즐 id가 주어지면 CANNOT_FIND_TRAINING_PUZZLE 예외 처리")
        @Test
        public void testDeleteTrainingPuzzle_PuzzleNotFound() {
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
        public void testSolveLessonPuzzle_PuzzleNotFound() {
            // given
            Long puzzleId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(Instant.now())
                    .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))  // deletedAt은 미래 시점으로 설정 (예시)
                    .status(Status.getDefaultStatus())
                    .build();

            // 기존 풀이 기록은 없으나, 퍼즐이 존재하지 않음
            when(solvedTrainingPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzleId))
                    .thenReturn(Optional.empty());
            when(trainingPuzzleRepository.findById(puzzleId))
                    .thenReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class,
                    () -> trainingService.solveTrainingPuzzle(user, puzzleId));

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
        public void testGetTrainingPuzzleList_PackNotFound() {
            // given
            Long packId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(Instant.now())
                    .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))  // deletedAt은 미래 시점으로 설정 (예시)
                    .status(Status.getDefaultStatus())
                    .build();

            when(trainingPuzzleRepository.findByPack_Id(packId)).thenReturn(Collections.emptyList());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    trainingService.getTrainingPuzzleList(user, packId)
            );
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_SUCH_TRAINING_PACK);

            // 불필요한 추가 호출이 없음을 확인 (원하는 경우)
            verifyNoMoreInteractions(trainingPuzzleRepository, solvedTrainingPuzzleRepository);
        }

        @Test
        @DisplayName("testGetTrainingPackList: Pack 목록이 비어 있으면 NO_SUCH_TRAINING_PACKS 예외 발생")
        public void testGetTrainingPackList_NoTrainingPack() {
            // given
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .lastAccessedAt(Instant.now())
                    .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))  // deletedAt은 미래 시점으로 설정 (예시)
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
        public void testPurchaseTrainingPack_NotEnoughCurrency() {
            // given
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .currency(500)
                    .lastAccessedAt(Instant.now())
                    .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))  // deletedAt은 미래 시점으로 설정 (예시)
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
        public void testPurchaseTrainingPuzzleAnswer_CannotPurchasePuzzle() {
            // given
            Long puzzleId = 1L;
            UserEntity user = UserEntity.builder()
                    .id(100L)
                    .email("test@example.com")
                    .password("password")
                    .nickname("testUser")
                    .deviceId("dummy-device")
                    .currency(500)
                    .lastAccessedAt(Instant.now())
                    .deletedAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .status(Status.getDefaultStatus())
                    .build();
            PurchaseTrainingPuzzleAnswerRequest request = new PurchaseTrainingPuzzleAnswerRequest(puzzleId);

            when(trainingPuzzleRepository.findById(eq(puzzleId)))
                    .thenReturn(Optional.empty());

            when(userRepository.findById(eq(user.getId())))
                    .thenReturn(Optional.of(user));

            // when
            CustomException exception = assertThrows(CustomException.class, () ->
                    trainingService.purchaseTrainingPuzzleAnswer(user, request)
            );

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE);
        }

    }

}
