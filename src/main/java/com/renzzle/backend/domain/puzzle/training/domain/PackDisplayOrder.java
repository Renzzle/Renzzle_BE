package com.renzzle.backend.domain.puzzle.training.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

// Higher priority first, then packs without a creator, then by creator in the order they started, then by id
public final class PackDisplayOrder {

    private PackDisplayOrder() {}

    // Creators are ranked among these, so pass every listed pack
    public static Comparator<Pack> of(Collection<Pack> packs) {
        Map<CreatorGroup, Long> firstPackIds = packs.stream()
                .collect(Collectors.toMap(CreatorGroup::of, Pack::getId, Math::min));

        return Comparator.comparingInt(Pack::getPriority).reversed()
                .thenComparing(pack -> pack.getCreatedBy() != null)
                .thenComparing(pack -> firstPackIds.getOrDefault(CreatorGroup.of(pack), pack.getId()))
                .thenComparing(Pack::getId);
    }

    private record CreatorGroup(String difficulty, Long createdBy) {
        static CreatorGroup of(Pack pack) {
            return new CreatorGroup(pack.getDifficulty().getName(), pack.getCreatedBy());
        }
    }

}
