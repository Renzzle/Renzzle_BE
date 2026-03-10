package com.renzzle.backend.domain.puzzle.play.domain;

import java.util.Map;

public interface SolutionSerializer {

    byte[] serialize(Map<Long, Integer> solutionDag);

    Map<Long, Integer> deserialize(byte[] serializedSolutionDag);
}
