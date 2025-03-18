package com.renzzle.backend.domain.puzzle.service;

import com.renzzle.backend.config.TestContainersConfig;
import com.renzzle.backend.domain.puzzle.api.request.CreateTrainingPackRequest;
import com.renzzle.backend.domain.puzzle.api.request.PackTranslationRequest;
import com.renzzle.backend.domain.puzzle.dao.PackRepository;
import com.renzzle.backend.domain.puzzle.dao.PackTranslationRepository;
import com.renzzle.backend.domain.puzzle.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.domain.Pack;
import com.renzzle.backend.domain.puzzle.domain.PackTranslation;
import com.renzzle.backend.global.common.constant.LanguageCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//@ActiveProfiles("test")
//@ContextConfiguration(initializers = TestContainersConfig.class)
public class TrainingServiceTest {


    @Mock
    private PackRepository packRepository;

    @Mock
    private PackTranslationRepository packTranslationRepository;

    @InjectMocks
    private TrainingService trainingService;


    @Test
    public void testCreatePack() {
        // Arrange: 요청 데이터를 생성 (record 타입은 생성자 호출과 동일)
        List<PackTranslationRequest> translationRequests = Arrays.asList(
                new PackTranslationRequest(LanguageCode.ko, "초보용 1", "강상민", "처음 퍼즐을 푸는..."),
                new PackTranslationRequest(LanguageCode.en, "For Beginner 1", "Kang Sang-Min", "First time to solve...")
        );
        CreateTrainingPackRequest request = new CreateTrainingPackRequest(translationRequests, 1000, "LOW");

        // 서비스 내부에서 새 Pack 객체를 생성하는데, 이때 puzzleCount는 0, price와 난이도는 요청 값으로 결정됨.
        Pack unsavedPack = Pack.builder()
                .price(request.price())
                .difficulty(Difficulty.getDifficulty(request.difficulty()))
                .puzzleCount(0)
                .build();
        // 저장 후 ID가 할당된 Pack 객체
        Pack savedPack = Pack.builder()
                .id(1L)
                .price(request.price())
                .difficulty(Difficulty.getDifficulty(request.difficulty()))
                .puzzleCount(0)
                .build();

        // 리포지토리의 save() 메소드 모킹
        when(packRepository.save(any(Pack.class))).thenReturn(savedPack);

        // 요청의 각 PackTranslationRequest를 기반으로 생성되는 PackTranslation 목록
        List<PackTranslation> expectedTranslations = request.info().stream()
                .map(info -> PackTranslation.builder()
                        .pack(savedPack)
                        .languageCode(info.langCode().name())
                        .title(info.title())
                        .author(info.author())
                        .description(info.description())
                        .build())
                .collect(Collectors.toList());

        // saveAll() 메소드도 모킹 (반환값은 상황에 따라 달라질 수 있음)
        when(packTranslationRepository.saveAll(anyList())).thenReturn(expectedTranslations);

        // Act: 서비스 메소드 호출
        Pack result = trainingService.createPack(request);

        // Assert: 반환값 및 리포지토리 호출 확인
        assertNotNull(result);
        assertEquals(savedPack.getId(), result.getId());
        verify(packRepository, times(1)).save(any(Pack.class));
        verify(packTranslationRepository, times(1)).saveAll(anyList());
    }


}
