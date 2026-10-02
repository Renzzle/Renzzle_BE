package com.renzzle.backend.global.exception;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void values_WhenCodesCompared_ThenNoTwoErrorsShareACode() {
        // clients branch on the code string, so a shared code makes two errors indistinguishable
        Map<String, List<String>> namesByCode = Arrays.stream(ErrorCode.values())
                .collect(Collectors.groupingBy(ErrorCode::getCode, TreeMap::new,
                        Collectors.mapping(Enum::name, Collectors.toList())));

        Map<String, List<String>> shared = namesByCode.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(shared).isEmpty();
    }
}
