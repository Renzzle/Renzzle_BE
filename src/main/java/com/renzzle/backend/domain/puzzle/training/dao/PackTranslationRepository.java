package com.renzzle.backend.domain.puzzle.training.dao;

import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import com.renzzle.backend.domain.puzzle.training.domain.PackTranslation;
import com.renzzle.backend.global.common.domain.LangCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackTranslationRepository extends JpaRepository<PackTranslation, Long> {
    List<PackTranslation> findAllByPack_IdInAndLangCode(List<Long> packIds, LangCode languageCode);

//    boolean existsByPackAndLanguageCode(Pack pack, String languageCode);

    Optional<PackTranslation> findByPack_IdAndLanguageCode(Long id, String name);

    boolean existsByPackAndLangCode(Pack pack, LangCode languageCode);

}
