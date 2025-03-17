package com.renzzle.backend.domain.puzzle.service;

import com.renzzle.backend.domain.puzzle.api.request.*;
import com.renzzle.backend.domain.puzzle.api.response.GetPackResponse;
import com.renzzle.backend.domain.puzzle.api.response.GetTrainingPuzzleAnswerResponse;
import com.renzzle.backend.domain.puzzle.api.response.GetTrainingPuzzleResponse;
import com.renzzle.backend.domain.puzzle.dao.*;
import com.renzzle.backend.domain.puzzle.domain.*;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.util.BoardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingPuzzleRepository trainingPuzzleRepository;
    private final SolvedTrainingPuzzleRepository solvedTrainingPuzzleRepository;
    private final PackRepository packRepository;
    private final PackTranslationRepository packTranslationRepository;
    private final UserPackRepository userPackRepository;
    private final UserRepository userRepository;

    @Transactional
    public TrainingPuzzle createTrainingPuzzle(AddTrainingPuzzleRequest request) {
        String boardKey = BoardUtils.makeBoardKey(request.boardStatus());

        int index = trainingPuzzleRepository.findTopIndex(request.packId()) + 1;
        if(request.puzzleIndex() != null && index > request.puzzleIndex()) {
            index = request.puzzleIndex();
            trainingPuzzleRepository.increaseIndexesFrom(request.packId(), index);
        }

        Pack pack = packRepository.findById(request.packId())
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK));

        double rating = request.depth() * 200;  // ELO 값 정해진 후 재정의 필요 !

        // increase puzzle_count
        packRepository.increasePuzzleCount(request.packId());

        TrainingPuzzle puzzle = TrainingPuzzle.builder()
                .pack(pack)
                .trainingIndex(index)
                .boardStatus(request.boardStatus())
                .boardKey(boardKey)
                .depth(request.depth())
                .rating(rating)
                .winColor(WinColor.getWinColor(request.winColor()))
                .build();

        return trainingPuzzleRepository.save(puzzle);
    }

    @Transactional
    public void deleteTrainingPuzzle(Long puzzleId) {
        Optional<TrainingPuzzle> puzzle = trainingPuzzleRepository.findById(puzzleId);
        if(puzzle.isEmpty())
            throw new CustomException(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE);

        trainingPuzzleRepository.deleteById(puzzleId);
        trainingPuzzleRepository.decreaseIndexesFrom(puzzle.get().getTrainingIndex());
    }

    @Transactional
    public void solveLessonPuzzle(UserEntity user, Long puzzleId) {
        Optional<SolvedTrainingPuzzle> existInfo
                = solvedTrainingPuzzleRepository.findByUserIdAndLessonId(user.getId(), puzzleId);

        // solve puzzle again
        if(existInfo.isPresent()) {
            // ALREADY_SOLVED_PUZZLE 라는 에러를 만들어야 할까?
            return;
        }

        TrainingPuzzle trainingPuzzle = trainingPuzzleRepository.findById(puzzleId).orElseThrow(
                () -> new CustomException(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE)
        );

        SolvedTrainingPuzzle solvedLessonPuzzle = SolvedTrainingPuzzle.builder()
                .user(user)
                .puzzle(trainingPuzzle)
                .build();
        solvedTrainingPuzzleRepository.save(solvedLessonPuzzle);
    }

    @Transactional(readOnly = true)
    public List<GetTrainingPuzzleResponse> getTrainingPuzzleList(UserEntity user, Long packId) {
        List<TrainingPuzzle> trainingPuzzles = trainingPuzzleRepository.findByPack_PackId(packId);

        if(trainingPuzzles.isEmpty()) {
            throw new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK);
        }

        List<GetTrainingPuzzleResponse> response = new ArrayList<>();
        trainingPuzzles.forEach(trainingPuzzle -> {
            boolean isSolved = solvedTrainingPuzzleRepository.existsByUserAndPuzzle(user, trainingPuzzle);

            response.add(GetTrainingPuzzleResponse.builder()
                    .id(trainingPuzzle.getId())
                    .boardStatus(trainingPuzzle.getBoardStatus())
                    .depth(trainingPuzzle.getDepth())
                    .winColor(trainingPuzzle.getWinColor().getName())
                    .isSolved(isSolved)
                    .build());
        });
        return response;
    }


    @Transactional
    public Pack createPack(CreatePackRequest request) {

        Pack pack = Pack.builder()
                .price(request.price())
                .difficulty(Difficulty.getDifficulty(request.difficulty()))
                .puzzle_count(0)
                .build();

        Pack savedPack = packRepository.save(pack);

        List<PackTranslation> translations = request.info().stream()
                .map(info -> PackTranslation.builder()
                        .pack(savedPack)
                        .language_code(info.langCode())
                        .title(info.title())
                        .author(info.author())
                        .description(info.description())
                        .build())
                .collect(Collectors.toList());

        packTranslationRepository.saveAll(translations);



        return savedPack;
    }

    @Transactional
    public void addTranslation(TranslationRequest request) {

        Pack pack = packRepository.findById(request.packId())
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK));

        PackTranslation translation = PackTranslation.builder()
                .pack(pack)
                .language_code(request.langCode())
                .title(request.title())
                .author(request.author())
                .description(request.description())
                .build();

        packTranslationRepository.save(translation);
    }

    @Transactional(readOnly = true)
    public List<GetPackResponse> getTrainingPackList(UserEntity user, String difficulty, String lang){

        List<Pack> packs = packRepository.findByDifficulty(Difficulty.getDifficulty(difficulty));

        List<Long> packIds = packs.stream().map(Pack::getId).collect(Collectors.toList());
        List<PackTranslation> translations = packTranslationRepository
                .findAllByPackIdInAndLanguageCode(packIds, lang);

        Long userId = user.getId();

        List<UserPack> userPacks = userPackRepository.findAllByUserIdAndPackIdIn(userId, packIds);
        Map<Long, UserPack> userPackMap = userPacks.stream()
                .collect(Collectors.toMap(up -> up.getPack().getId(), up -> up));

        Map<Long, PackTranslation> translationMap = translations.stream()
                .collect(Collectors.toMap(
                        t -> t.getPack().getId(),  // key: packId
                        t -> t                     // value: PackTranslation 객체
                ));

        List<GetPackResponse> result = new ArrayList<>();
        for (Pack pack : packs) {
            PackTranslation translation = translationMap.get(pack.getId());
            UserPack up = userPackMap.get(pack.getId());

            // locked 여부, solvedPuzzleCount 계산
            boolean locked = (up == null);
            int solvedCount = (up != null) ? up.getSolved_count() : 0;

            GetPackResponse dto = new GetPackResponse(
                    pack.getId(),
                    translation != null ? translation.getTitle() : null,
                    translation != null ? translation.getAuthor() : null,
                    translation != null ? translation.getDescription() : null,
                    pack.getPrice(),
                    pack.getPuzzle_count(),
                    solvedCount,
                    locked
            );
            result.add(dto);
        }

        return result;
    }

    //purchase 시 currentCurrency < price 체크하는 부분은 엔티티쪽으로 코드 옮기기
    @Transactional(readOnly = true)
    public Integer purchaseTrainingPack(UserEntity user, PurchaseTrainingPackRequest request) {

        Pack pack = packRepository.findById(request.packId())
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK));

        int price = pack.getPrice();
        int currentCurrency = user.getCurrency();

        if (currentCurrency < price) {
            throw new CustomException(ErrorCode.INSUFFICIENT_CURRENCY);
        }

        int newCurrency = currentCurrency - price;
        user.setCurrency(newCurrency);
        userRepository.save(user);

        UserPack userPack = UserPack.builder()
                .user(user)
                .pack(pack)
                .solved_count(0)
                .build();

        userPackRepository.save(userPack);

        return newCurrency;

    }

    @Transactional(readOnly = true)
    public GetTrainingPuzzleAnswerResponse purchaseTrainingPuzzleAnswer(UserEntity user, PurchaseTrainingPuzzleAnswerRequest request) {

        TrainingPuzzle puzzle = trainingPuzzleRepository.findById(request.puzzleId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE));

        int currentCurrency = user.getCurrency();
        if (currentCurrency < 100) {
            throw new CustomException(ErrorCode.INSUFFICIENT_CURRENCY);
        }

        int newCurrency = currentCurrency - 100;
        user.setCurrency(newCurrency);
        userRepository.save(user);

        GetTrainingPuzzleAnswerResponse response = GetTrainingPuzzleAnswerResponse.builder()
                .answer(puzzle.getAnswer())
                .currency(newCurrency)
                .build();

        return response;
    }

}
