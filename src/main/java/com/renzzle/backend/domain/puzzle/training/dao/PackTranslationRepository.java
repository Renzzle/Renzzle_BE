package com.renzzle.backend.domain.puzzle.training.dao;

import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import com.renzzle.backend.domain.puzzle.training.domain.PackTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackTranslationRepository extends JpaRepository<PackTranslation, Long> {
    List<PackTranslation> findAllByPack_IdInAndLanguageCode(List<Long> packIds, String languageCode);

    boolean existsByPackAndLanguageCode(Pack pack, String languageCode);
}
