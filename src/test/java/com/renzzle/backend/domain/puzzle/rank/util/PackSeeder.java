package com.renzzle.backend.domain.puzzle.rank.util;

import com.renzzle.backend.domain.puzzle.training.dao.PackRepository;
import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
//@Profile("test")
public class PackSeeder {

    @Autowired
    private PackRepository packRepository;

    public Pack seedPack(String difficultyName, int puzzleCount, int price) {

        return packRepository.save(
                Pack.builder()
                        .difficulty(Difficulty.getDifficulty(difficultyName))
                        .puzzleCount(puzzleCount)
                        .price(price)
                        .build()
        );
    }
}
