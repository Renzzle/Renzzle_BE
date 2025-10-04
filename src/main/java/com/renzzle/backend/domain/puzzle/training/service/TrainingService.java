package com.renzzle.backend.domain.puzzle.training.service;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.training.api.response.GetPackPurchaseResponse;
import com.renzzle.backend.domain.puzzle.training.api.response.GetPackResponse;
import com.renzzle.backend.domain.puzzle.training.api.response.GetTrainingPuzzleAnswerResponse;
import com.renzzle.backend.domain.puzzle.training.api.response.GetTrainingPuzzleResponse;
import com.renzzle.backend.domain.puzzle.training.api.request.*;
import com.renzzle.backend.domain.puzzle.training.api.response.SolveTrainingPuzzleResponse;
import com.renzzle.backend.domain.puzzle.training.dao.*;
import com.renzzle.backend.domain.puzzle.training.domain.*;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.constant.ItemPrice;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.renzzle.backend.global.common.constant.DoubleConstant.DEFAULT_PUZZLE_RATING;
import static com.renzzle.backend.global.common.constant.ItemPrice.*;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingPuzzleRepository trainingPuzzleRepository;
    private final SolvedTrainingPuzzleRepository solvedTrainingPuzzleRepository;
    private final PackRepository packRepository;
    private final PackTranslationRepository packTranslationRepository;
    private final UserPackRepository userPackRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    // service test, repo test
    @Transactional
    public TrainingPuzzle createTrainingPuzzle(AddTrainingPuzzleRequest request) {

        Pack pack = packRepository.findById(request.packId())
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK));

        String boardKey = BoardUtils.makeBoardKey(request.boardStatus());

        int index = trainingPuzzleRepository.findTopIndex(request.packId()) + 1;
        if(index > request.puzzleIndex()) {
            index = request.puzzleIndex();
            trainingPuzzleRepository.increaseIndexesFrom(request.packId(), index);
        }

        double rating = request.depth() * DEFAULT_PUZZLE_RATING;

        // increase puzzle_count
        packRepository.increasePuzzleCount(request.packId());

        TrainingPuzzle puzzle = TrainingPuzzle.builder()
                .pack(pack)
                .trainingIndex(index)
                .answer(request.answer())
                .boardStatus(request.boardStatus())
                .boardKey(boardKey)
                .depth(request.depth())
                .rating(rating)
                .winColor(WinColor.getWinColor(request.winColor()))
                .build();

        return trainingPuzzleRepository.save(puzzle);
    }

    // service test, repo test
    @Transactional
    public void deleteTrainingPuzzle(Long puzzleId) {
        Optional<TrainingPuzzle> puzzle = trainingPuzzleRepository.findById(puzzleId);
        if(puzzle.isEmpty())
            throw new CustomException(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE);

        Pack pack = puzzle.get().getPack();

        List<SolvedTrainingPuzzle> solvedRecords = solvedTrainingPuzzleRepository.findAllByPuzzleId(puzzleId);

        for (SolvedTrainingPuzzle solved : solvedRecords) {
            Long userId = solved.getUser().getId();
            userPackRepository.decreaseSolvedCount(userId, pack.getId());
        }

        trainingPuzzleRepository.deleteById(puzzleId);
        trainingPuzzleRepository.decreaseIndexesFrom(puzzle.get().getTrainingIndex());

        packRepository.decreasePuzzleCount(puzzle.get().getPack().getId());
    }

    // service test, repo test
    @Transactional
    public SolveTrainingPuzzleResponse solveTrainingPuzzle(UserEntity user, Long puzzleId, Boolean getReward) {
        Optional<SolvedTrainingPuzzle> existInfo =
                solvedTrainingPuzzleRepository.findByUserIdAndPuzzleId(user.getId(), puzzleId);

        if (existInfo.isPresent()) {
            existInfo.get().updateSolvedAtToNow(clock);
            return SolveTrainingPuzzleResponse.builder()
                    .reward(0)
                    .build();
        }

        TrainingPuzzle trainingPuzzle = trainingPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE));

        // 처음 푼 퍼즐이면 저장 및 난이도에 따른 보상 계산
        solvedTrainingPuzzleRepository.save(SolvedTrainingPuzzle.builder()
                .user(user)
                .puzzle(trainingPuzzle)
                .build());

        userPackRepository.increaseSolvedCount(user.getId(), trainingPuzzle.getPack().getId());

        // 난이도 → 보상 매핑
        Difficulty difficulty = trainingPuzzle.getPack().getDifficulty();
        int reward = switch (difficulty.getName()) {
            case "LOW" -> TRAINING_LOW_REWARD.getPrice();
            case "MIDDLE" -> TRAINING_MIDDLE_REWARD.getPrice();
            case "HIGH" -> TRAINING_HIGH_REWARD.getPrice();
            default -> 0;
        };
        if(getReward) user.getReward(reward);

        return SolveTrainingPuzzleResponse.builder()
                .reward(reward)
                .build();
    }

    // service test, repo test
    @Transactional(readOnly = true)
    public List<GetTrainingPuzzleResponse> getTrainingPuzzleList(UserEntity user, Long packId) {
        if(packId == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR);
        }

        //TODO : training index 순서대로 반환할 수 있도록
        List<TrainingPuzzle> trainingPuzzles = trainingPuzzleRepository.findByPack_IdOrderByTrainingIndex(packId);

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


    // service test, repo test
    @Transactional
    public Pack createPack(CreateTrainingPackRequest request) {
        Pack pack = Pack.builder()
                .price(request.price())
                .difficulty(Difficulty.getDifficulty(request.difficulty()))
                .puzzleCount(0)
                .build();

        Pack savedPack = packRepository.save(pack);

        List<PackTranslation> translations = request.info().stream()
                .map(info -> PackTranslation.builder()
                        .pack(savedPack)
                        .langCode(LangCode.getLangCode(info.langCode()))
                        .title(info.title())
                        .author(info.author())
                        .description(info.description())
                        .build())
                .collect(Collectors.toList());

        packTranslationRepository.saveAll(translations);

        return savedPack;
    }

    // service test, repo test
    @Transactional
    public void addTranslation(TranslationRequest request) {
        Pack pack = packRepository.findById(request.packId())
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK));

        boolean exists = packTranslationRepository.existsByPackAndLangCode(pack, LangCode.getLangCode(request.langCode()));

        if (exists) {
            throw new CustomException(ErrorCode.ALREADY_EXISTING_TRANSLATION);
        }

        PackTranslation translation = PackTranslation.builder()
                .pack(pack)
                .langCode(LangCode.getLangCode(request.langCode()))
                .title(request.title())
                .author(request.author())
                .description(request.description())
                .build();

        packTranslationRepository.save(translation);
    }

    // service test, repo test
    @Transactional(readOnly = true)
    public List<GetPackResponse> getTrainingPackList(UserEntity user, GetTrainingPackRequest request){
        List<Pack> packs = packRepository.findByDifficulty(Difficulty.getDifficulty(request.difficulty()));

        if (packs.isEmpty()) {
            throw new CustomException(ErrorCode.NO_SUCH_TRAINING_PACKS);
        }

        List<Long> packIds = packs.stream().map(Pack::getId).collect(Collectors.toList());
        LangCode requestedLangCode = LangCode.getLangCode(request.lang());
        LangCode defaultLangCode = LangCode.getLangCode(LangCode.LangCodeName.EN);

        List<PackTranslation> requestedTranslations =
                packTranslationRepository.findAllByPack_IdInAndLangCode(packIds, requestedLangCode);
        List<PackTranslation> defaultTranslations =
                packTranslationRepository.findAllByPack_IdInAndLangCode(packIds, defaultLangCode);

        Map<Long, PackTranslation> requestedMap = requestedTranslations.stream()
                .collect(Collectors.toMap(t -> t.getPack().getId(), t -> t));
        Map<Long, PackTranslation> defaultMap = defaultTranslations.stream()
                .collect(Collectors.toMap(t -> t.getPack().getId(), t -> t));

        Long userId = user.getId();

        List<UserPack> userPacks = userPackRepository.findAllByUserIdAndPackIdIn(userId, packIds);
        Map<Long, UserPack> userPackMap = userPacks.stream()
                .collect(Collectors.toMap(up -> up.getPack().getId(), up -> up));

        List<GetPackResponse> result = new ArrayList<>();
        for (Pack pack : packs) {
            PackTranslation translation =
                    requestedMap.getOrDefault(pack.getId(), defaultMap.get(pack.getId()));

            UserPack up = userPackMap.get(pack.getId());
            boolean locked = (up == null);
            int solvedCount = (up != null) ? up.getSolvedCount() : 0;

            GetPackResponse dto = new GetPackResponse(
                    pack.getId(),
                    translation != null ? translation.getTitle() : null,
                    translation != null ? translation.getAuthor() : null,
                    translation != null ? translation.getDescription() : null,
                    pack.getPrice(),
                    pack.getPuzzleCount(),
                    solvedCount,
                    locked
            );
            result.add(dto);
        }

        return result;
    }

    // service test, repo test
    @Transactional
    public GetPackPurchaseResponse purchaseTrainingPack(UserEntity user, PurchaseTrainingPackRequest request) {
        Pack pack = packRepository.findById(request.packId())
                .orElseThrow(() -> new CustomException(ErrorCode.NO_SUCH_TRAINING_PACK));

        user.purchase(pack.getPrice());

        userRepository.save(user);

        UserPack userPack = UserPack.builder()
                .user(user)
                .pack(pack)
                .solvedCount(0)
                .build();

        userPackRepository.save(userPack);
        return GetPackPurchaseResponse.builder()
                .price(pack.getPrice())
                .build();
    }

    @Transactional
    public GetTrainingPuzzleAnswerResponse purchaseTrainingPuzzleAnswer(UserEntity user, PurchaseTrainingPuzzleAnswerRequest request) {
        UserEntity newUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));
        TrainingPuzzle puzzle = trainingPuzzleRepository.findById(request.puzzleId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE));

        newUser.purchase(ItemPrice.HINT.getPrice());

        solveTrainingPuzzle(user, puzzle.getId(), false);

        return GetTrainingPuzzleAnswerResponse.builder()
                .answer(puzzle.getAnswer())
                .price(ItemPrice.HINT.getPrice())
                .build();
    }
}
