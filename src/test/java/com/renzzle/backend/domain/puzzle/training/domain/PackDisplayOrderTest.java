package com.renzzle.backend.domain.puzzle.training.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PackDisplayOrderTest {

    private static final Long CREATOR_A = 10L;
    private static final Long CREATOR_B = 20L;
    private static final Long CREATOR_C = 30L;

    @Test
    void of_WhenPrioritiesTie_ThenOperatorPacksLeadAndCreatorsFollowInTheOrderTheyStarted() {
        List<Pack> packs = List.of(
                pack(1L, "LOW", null, 0),
                pack(2L, "LOW", CREATOR_A, 0),
                pack(3L, "LOW", CREATOR_B, 0),
                pack(4L, "LOW", CREATOR_A, 0),
                pack(5L, "LOW", null, 0),
                pack(6L, "LOW", CREATOR_C, 0),
                pack(7L, "LOW", CREATOR_B, 0));

        assertThat(sortedIds(packs)).containsExactly(1L, 5L, 2L, 4L, 3L, 7L, 6L);
    }

    @Test
    void of_WhenPrioritySet_ThenHigherComesFirstAndNegativeGoesLast() {
        List<Pack> packs = List.of(
                pack(1L, "LOW", null, -1),
                pack(2L, "LOW", null, 0),
                pack(3L, "LOW", CREATOR_A, 5),
                pack(4L, "LOW", CREATOR_B, 10));

        assertThat(sortedIds(packs)).containsExactly(4L, 3L, 2L, 1L);
    }

    @Test
    void of_WhenCreatorStartedInAnotherDifficulty_ThenRanksThemByTheirFirstPackOfThisOne() {
        List<Pack> packs = List.of(
                pack(1L, "HIGH", CREATOR_A, 0),
                pack(2L, "LOW", CREATOR_B, 0),
                pack(3L, "LOW", CREATOR_A, 0),
                pack(4L, "LOW", CREATOR_B, 0));

        List<Pack> low = packs.stream().filter(pack -> pack.getDifficulty().getName().equals("LOW")).toList();
        assertThat(low.stream().sorted(PackDisplayOrder.of(packs)).map(Pack::getId).toList())
                .containsExactly(2L, 4L, 3L);
    }

    private static List<Long> sortedIds(List<Pack> packs) {
        return packs.stream().sorted(PackDisplayOrder.of(packs)).map(Pack::getId).toList();
    }

    private static Pack pack(Long id, String difficulty, Long createdBy, int priority) {
        return Pack.builder()
                .id(id)
                .difficulty(Difficulty.getDifficulty(difficulty))
                .createdBy(createdBy)
                .priority(priority)
                .build();
    }

}
