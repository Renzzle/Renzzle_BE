package com.renzzle.backend.domain.puzzle.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.api.request.CreateTrainingPackRequest;
import com.renzzle.backend.domain.puzzle.api.request.PackTranslationRequest;
import com.renzzle.backend.domain.puzzle.dao.*;
import com.renzzle.backend.domain.puzzle.domain.*;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.constant.ItemPrice;
import com.renzzle.backend.global.common.constant.LanguageCode;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.init.DataInitializer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class TrainingServiceRepositoryTest {

    @Autowired
    private PackRepository packRepository;

    @Autowired
    private TrainingPuzzleRepository trainingPuzzleRepository;

    @Autowired
    private SolvedTrainingPuzzleRepository solvedTrainingPuzzleRepository;

    @Autowired
    private PackTranslationRepository packTranslationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPackRepository userPackRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    public void clearDatabase() {
        userRepository.deleteAll();
        entityManager.createNativeQuery("DELETE FROM difficulty").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM status").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM win_color").executeUpdate();
        entityManager.clear();
    }

    private UserEntity persistTestUser() {

        Status defaultStatus = Status.getDefaultStatus();
        entityManager.persist(defaultStatus);

        UserEntity user = UserEntity.builder()
                .email("testuser@example.com")
                .password("password")
                .nickname("testUser")
                .deviceId("device123")
                .lastAccessedAt(Instant.now())
                .deletedAt(Instant.now().plusSeconds(3600))
                // status는 @PrePersist에서 기본값이 설정되도록 되어있으므로 null로 둡니다.
                .build();
        entityManager.persist(user);
        return user;
    }

    @Test
    @DisplayName("TrainingPuzzle 저장 및 조회: 유효한 Pack과 TrainingPuzzle을 저장 후 정상 조회")
    @Transactional
    public void testCreateTrainingPuzzle() {
        // Given
        Difficulty difficulty = Difficulty.getDifficulty("LOW");
        entityManager.persist(difficulty);

        WinColor winColor = WinColor.getWinColor("WHITE");
        entityManager.persist(winColor);

        Pack pack = Pack.builder()
                .price(1000)
                .difficulty(difficulty)
                .puzzleCount(0)
                .build();
        Pack savedPack = packRepository.save(pack);

        // Given
        TrainingPuzzle existingPuzzle = TrainingPuzzle.builder()
                .pack(savedPack)
                .trainingIndex(0)
                .boardStatus("existingStatus")
                .boardKey("existingKey")
                .answer("existing answer")
                .depth(3)
                .rating(600.0)
                .winColor(winColor)
                .build();
        TrainingPuzzle savedExistingPuzzle = trainingPuzzleRepository.save(existingPuzzle);

        // When
        trainingPuzzleRepository.increaseIndexesFrom(savedPack.getId(), 0);

        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<TrainingPuzzle> updatedPuzzleOpt = trainingPuzzleRepository.findById(savedExistingPuzzle.getId());
        assertThat(updatedPuzzleOpt).isPresent();
        TrainingPuzzle updatedPuzzle = updatedPuzzleOpt.get();
        assertThat(updatedPuzzle.getTrainingIndex()).isEqualTo(1);

    }

    @Test
    @DisplayName("testDeleteAndDecreaseIndexes: 존재하는 퍼즐 삭제 후, 후속 퍼즐의 trainingIndex가 감소된다.")
    @Transactional
    public void testDeleteAndDecreaseIndexes() {
        // Given
        Difficulty difficulty = Difficulty.getDifficulty("LOW");
        entityManager.persist(difficulty);

        WinColor winColor = WinColor.getWinColor("WHITE");
        entityManager.persist(winColor);

        // Pack 생성 및 저장
        Pack pack = Pack.builder()
                .price(1000)
                .difficulty(difficulty)
                .puzzleCount(2)
                .build();
        Pack savedPack = packRepository.save(pack);

        // 두 개의 TrainingPuzzle 생성 (같은 Pack)
        // puzzle1: trainingIndex 1, puzzle2: trainingIndex 2
        TrainingPuzzle puzzle1 = TrainingPuzzle.builder()
                .pack(savedPack)
                .trainingIndex(1)
                .boardStatus("status1")
                .boardKey("key1")
                .answer("answer1")
                .depth(3)
                .rating(600.0)
                .winColor(winColor)
                .build();

        TrainingPuzzle puzzle2 = TrainingPuzzle.builder()
                .pack(savedPack)
                .trainingIndex(2)
                .boardStatus("status2")
                .boardKey("key2")
                .answer("answer2")
                .depth(3)
                .rating(600.0)
                .winColor(winColor)
                .build();

        TrainingPuzzle savedPuzzle1 = trainingPuzzleRepository.save(puzzle1);
        TrainingPuzzle savedPuzzle2 = trainingPuzzleRepository.save(puzzle2);


        // When
        trainingPuzzleRepository.deleteById(savedPuzzle1.getId());
        trainingPuzzleRepository.decreaseIndexesFrom(savedPuzzle1.getTrainingIndex());
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<TrainingPuzzle> deletedPuzzle = trainingPuzzleRepository.findById(savedPuzzle1.getId());

        assertThat(deletedPuzzle).isNotPresent();

        entityManager.flush();
        entityManager.clear();

        TrainingPuzzle updatedPuzzle2 = trainingPuzzleRepository.findById(savedPuzzle2.getId())
                .orElseThrow();
        assertThat(updatedPuzzle2.getTrainingIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("testSaveAndFindSolvedTrainingPuzzle: SolvedTrainingPuzzle 저장 후, findByUserIdAndPuzzleId로 조회")
    @Transactional
    public void testSolveTrainingPuzzle() {
        // Given
        Difficulty difficulty = Difficulty.getDifficulty("LOW");
        entityManager.persist(difficulty);

        WinColor winColor = WinColor.getWinColor("WHITE");
        entityManager.persist(winColor);

        Pack pack = Pack.builder()
                .price(1000)
                .difficulty(difficulty)
                .puzzleCount(0)
                .build();
        Pack savedPack = packRepository.save(pack);

        TrainingPuzzle puzzle = TrainingPuzzle.builder()
                .pack(savedPack)
                .trainingIndex(1)
                .boardStatus("a1a3d8f9") // BoardUtils에서 유효한 값이어야 함
                .boardKey("sampleKey")
                .answer("sample answer")
                .depth(3)
                .rating(600.0)
                .winColor(winColor)
                .build();
        TrainingPuzzle savedPuzzle = trainingPuzzleRepository.save(puzzle);

        UserEntity user = persistTestUser();

        SolvedTrainingPuzzle solved = SolvedTrainingPuzzle.builder()
                .user(user)
                .puzzle(savedPuzzle)
                .build();
        solvedTrainingPuzzleRepository.save(solved);

        // When
        Optional<SolvedTrainingPuzzle> found = solvedTrainingPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), savedPuzzle.getId());

        // Then: 조회 결과가 존재하고, 올바른 user와 puzzle이 연결되어 있는지 확인
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(found.get().getPuzzle().getId()).isEqualTo(savedPuzzle.getId());
    }

    @Test
    @DisplayName("GetTrainingPuzzleList: 특정 Pack의 TrainingPuzzle 목록이 정상적으로 조회된다.")
    @Transactional
    public void testGetTrainingPuzzleList() {
        // Given

        Difficulty difficulty = Difficulty.getDifficulty("LOW");
        entityManager.persist(difficulty);
////        entityManager.flush();

        WinColor winColor = WinColor.getWinColor("WHITE");
        entityManager.persist(winColor);
////        entityManager.flush();

        Pack pack = Pack.builder()
                .price(1000)
                .difficulty(difficulty)
                .puzzleCount(0)
                .build();
        Pack savedPack = packRepository.save(pack);
////        entityManager.flush();

        TrainingPuzzle puzzle1 = TrainingPuzzle.builder()
                .pack(savedPack)
                .trainingIndex(1)
                .boardStatus("status1")
                .boardKey("key1")
                .answer("answer1")
                .depth(3)
                .rating(600.0)
                .winColor(winColor)
                .build();

        TrainingPuzzle puzzle2 = TrainingPuzzle.builder()
                .pack(savedPack)
                .trainingIndex(2)
                .boardStatus("status2")
                .boardKey("key2")
                .answer("answer2")
                .depth(3)
                .rating(600.0)
                .winColor(winColor)
                .build();

        trainingPuzzleRepository.save(puzzle1);
        trainingPuzzleRepository.save(puzzle2);
////        entityManager.flush();
////        entityManager.clear(); // 최신 DB 상태 반영을 위해 캐시 비움

        // When
        List<TrainingPuzzle> puzzles = trainingPuzzleRepository.findByPack_Id(savedPack.getId());

        // Then
        assertThat(puzzles).isNotEmpty();
        assertThat(puzzles).hasSize(2);
        assertThat(puzzles.get(0).getBoardStatus()).isIn("status1", "status2");
    }

    @Test
    @DisplayName("createPack: 유효한 요청으로 Pack과 PackTranslation이 정상 저장되고 조회")
    @Transactional
    public void testCreatePack() {
        // Given
        Difficulty difficulty = Difficulty.getDifficulty("LOW");
        entityManager.persist(difficulty);
////        entityManager.flush();

        List<PackTranslationRequest> translations = Arrays.asList(
                new PackTranslationRequest(LanguageCode.ko, "초보용 1", "재윤", "설명1"),
                new PackTranslationRequest(LanguageCode.en, "For Beginner 1", "JaeYun", "Description1")
        );
        CreateTrainingPackRequest request = new CreateTrainingPackRequest(translations, 1000, "LOW");

        Pack pack = Pack.builder()
                .price(request.price())
                .difficulty(Difficulty.getDifficulty(request.difficulty()))
                .puzzleCount(0)
                .build();

        // When
        Pack savedPack = packRepository.save(pack);
////        entityManager.flush();

        // PackTranslation 생성 및 저장
        List<PackTranslation> packTranslations = request.info().stream()
                .map(info -> PackTranslation.builder()
                        .pack(savedPack)
                        .languageCode(info.langCode().name())
                        .title(info.title())
                        .author(info.author())
                        .description(info.description())
                        .build())
                .collect(Collectors.toList());
        packTranslationRepository.saveAll(packTranslations);
//        entityManager.flush();
//        entityManager.clear();  // 최신 DB 상태 반영을 위해 영속성 컨텍스트 초기화

        // Then: 저장된 Pack과 PackTranslation 검증
        Optional<Pack> retrievedPackOpt = packRepository.findById(savedPack.getId());
        assertThat(retrievedPackOpt).isPresent();
        Pack retrievedPack = retrievedPackOpt.get();
        assertThat(retrievedPack.getPrice()).isEqualTo(1000);
        assertThat(retrievedPack.getDifficulty().getName()).isEqualTo("LOW");

        // 번역은 해당 Pack에 연관된 데이터여야 함 (연관관계 기준으로 조회하거나 전체 조회 후 필터링)
        // 여기서는 packTranslationRepository.findAll()을 사용한 후, packId가 일치하는지 확인
        List<PackTranslation> retrievedTranslations = packTranslationRepository.findAll().stream()
                .filter(pt -> pt.getPack().getId().equals(retrievedPack.getId()))
                .collect(Collectors.toList());
        assertThat(retrievedTranslations).hasSize(2);
        retrievedTranslations.forEach(pt ->
                assertThat(pt.getPack().getId()).isEqualTo(retrievedPack.getId())
        );
    }

    @Test
    @DisplayName("getTrainingPackList : findByDifficulty, findAllByPack_IdInAndLanguageCode, findAllByUserIdAndPackIdIn 메소드 확인")
    @Transactional
    public void testGetTrainingPackList() {
        // Given: Difficulty, WinColor 영속화
        Difficulty difficulty = Difficulty.getDifficulty("LOW");
        entityManager.persist(difficulty);
//        entityManager.flush();

        WinColor winColor = WinColor.getWinColor("WHITE");
        entityManager.persist(winColor);
//        entityManager.flush();

        // Pack 생성 및 저장
        Pack pack = Pack.builder()
                .price(1000)
                .difficulty(difficulty)
                .puzzleCount(10)
                .build();
        Pack savedPack = packRepository.save(pack);
//        entityManager.flush();

        // PackTranslation 생성 및 저장 (언어 코드 "EN")
        PackTranslation translation = PackTranslation.builder()
                .pack(savedPack)
                .languageCode(LanguageCode.en.name())
                .title("Test Title")
                .author("Test Author")
                .description("Test Description")
                .build();
        packTranslationRepository.save(translation);
//        entityManager.flush();

        // When
        List<Pack> packs = packRepository.findByDifficulty(difficulty);
        List<Long> packIds = packs.stream().map(Pack::getId).collect(Collectors.toList());
        List<PackTranslation> translations = packTranslationRepository.findAllByPack_IdInAndLanguageCode(packIds, LanguageCode.en.name());

        // Then
        assertThat(packs).hasSize(1);
        assertThat(packs.get(0).getId()).isEqualTo(savedPack.getId());
        assertThat(translations).hasSize(1);
        PackTranslation retrievedTranslation = translations.get(0);
        assertThat(retrievedTranslation.getTitle()).isEqualTo("Test Title");
        assertThat(retrievedTranslation.getAuthor()).isEqualTo("Test Author");
        assertThat(retrievedTranslation.getDescription()).isEqualTo("Test Description");
    }

    @Test
    @DisplayName("testAddTranslation: 유효한 Pack에 대해 번역을 추가 시 저장")
    @Transactional
    public void testAddTranslation() {
        // Given
        Difficulty difficulty = Difficulty.getDifficulty("LOW");
        entityManager.persist(difficulty);
//        entityManager.flush();

        Pack pack = Pack.builder()
                .price(1000)
                .difficulty(difficulty)
                .puzzleCount(0)
                .build();
        Pack savedPack = packRepository.save(pack);
//        entityManager.flush();
        boolean existsBefore = packTranslationRepository.existsByPackAndLanguageCode(savedPack, LanguageCode.en.name());
        assertThat(existsBefore).isFalse();

        // When
        PackTranslation translation = PackTranslation.builder()
                .pack(savedPack)
                .languageCode(LanguageCode.en.name())   // request.langCode().name()의 결과로 "EN"이라 가정
                .title("Test Title")
                .author("Test Author")
                .description("Test Description")
                .build();
        packTranslationRepository.save(translation);
//        entityManager.flush();
//        entityManager.clear(); // 최신 DB 상태 반영

        // Then
        List<PackTranslation> translations = packTranslationRepository.findAll();
        assertThat(translations).hasSize(1);
        PackTranslation retrieved = translations.get(0);
        assertThat(retrieved.getPack().getId()).isEqualTo(savedPack.getId());
        assertThat(retrieved.getLanguageCode()).isEqualTo(LanguageCode.en.name());
        assertThat(retrieved.getTitle()).isEqualTo("Test Title");
        assertThat(retrieved.getAuthor()).isEqualTo("Test Author");
        assertThat(retrieved.getDescription()).isEqualTo("Test Description");
    }

    @Test
    @DisplayName("testPurchaseTrainingPack: 사용자의 잔액 차감 및 UserPack 저장")
    @Transactional
    public void testPurchaseTrainingPack() {
        // Given
        Difficulty difficulty = Difficulty.getDifficulty("LOW");
        entityManager.persist(difficulty);
//        entityManager.flush();

        Status defaultStatus = Status.getDefaultStatus();
        entityManager.persist(defaultStatus);
//        entityManager.flush();

        Pack pack = Pack.builder()
                .price(1000)
                .difficulty(difficulty)
                .puzzleCount(0)
                .build();
        Pack savedPack = packRepository.save(pack);
//        entityManager.flush();

        UserEntity user = UserEntity.builder()
                .email("test@example.com")
                .password("password")
                .nickname("testUser")
                .deviceId("device123")
                .status(defaultStatus)
                .lastAccessedAt(Instant.now())
                .deletedAt(Instant.now().plusSeconds(3600))
                .currency(2000)
                .build();

        user = userRepository.save(user);
//        entityManager.flush();

        // When
        user.purchase(savedPack.getPrice());
        user = userRepository.save(user);

        UserPack userPack = UserPack.builder()
                .user(user)
                .pack(savedPack)
                .solved_count(0)
                .build();
        userPackRepository.save(userPack);
//        entityManager.flush();
//        entityManager.clear();

        UserEntity updatedUser = userRepository.findById(user.getId()).orElseThrow();

        // Then
        assertThat(updatedUser.getCurrency()).isEqualTo(1000);

        List<UserPack> userPacks = userPackRepository.findAllByUserIdAndPackIdIn(updatedUser.getId(),
                List.of(savedPack.getId()));
        assertThat(userPacks).hasSize(1);
        assertThat(userPacks.get(0).getSolved_count()).isEqualTo(0);
    }

    @Test
    @DisplayName("testPurchaseTrainingPuzzleAnswer: 사용자의 잔액이 정상 차감되고, 구매 후 결과를 조회할 수 있다.")
    @Transactional
    public void testPurchaseTrainingPuzzleAnswer() {
        // Given
        Difficulty difficulty = Difficulty.getDifficulty("LOW");
        entityManager.persist(difficulty);
//        entityManager.flush();

        Status defaultStatus = Status.getDefaultStatus();
        entityManager.persist(defaultStatus);
//        entityManager.flush();

        Pack pack = Pack.builder()
                .price(1000)
                .puzzleCount(0)
                .difficulty(difficulty)
                .build();
        packRepository.save(pack);
//        entityManager.flush();

        UserEntity user = UserEntity.builder()
                .email("user@example.com")
                .password("password")
                .nickname("user1")
                .deviceId("device1")
                .lastAccessedAt(Instant.now())
                .deletedAt(Instant.now().plusSeconds(3600))
                .status(defaultStatus)
                .currency(500)
                .build();
        user = userRepository.save(user);
//        entityManager.flush();
//        entityManager.clear();

        // When
        user.purchase(ItemPrice.HINT.getPrice());
        user = userRepository.save(user);
//        entityManager.flush();
//        entityManager.clear();

        // Then
        UserEntity updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getCurrency()).isEqualTo(500 - ItemPrice.HINT.getPrice());
    }

}
