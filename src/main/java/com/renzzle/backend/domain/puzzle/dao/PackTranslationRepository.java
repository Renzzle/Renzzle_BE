package com.renzzle.backend.domain.puzzle.dao;

import com.renzzle.backend.domain.puzzle.domain.PackTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackTranslationRepository extends JpaRepository<PackTranslation, Long> {
    List<PackTranslation> findAllByPackIdInAndLanguageCode(List<Long> packIds, String lang);
}
