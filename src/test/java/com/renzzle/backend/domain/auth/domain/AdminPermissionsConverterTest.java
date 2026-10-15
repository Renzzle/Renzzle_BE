package com.renzzle.backend.domain.auth.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static com.renzzle.backend.domain.auth.domain.AdminPermission.*;
import static org.assertj.core.api.Assertions.assertThat;

class AdminPermissionsConverterTest {

    private final AdminPermissionsConverter converter = new AdminPermissionsConverter();

    @Test
    void convertToDatabaseColumn_WhenGivenInAnyOrder_ThenWritesThemInEnumOrder() {
        assertThat(converter.convertToDatabaseColumn(Set.of(OPERATION_MANAGE, PACK_MANAGE)))
                .isEqualTo("PACK_MANAGE,OPERATION_MANAGE");
        assertThat(converter.convertToDatabaseColumn(EnumSet.noneOf(AdminPermission.class))).isEmpty();
    }

    @Test
    void convertToEntityAttribute_WhenColumnHasUnknownNames_ThenKeepsOnlyKnownPermissions() {
        assertThat(converter.convertToEntityAttribute("PACK_MANAGE, REMOVED_PERMISSION,AI_RESPONSE_MANAGE"))
                .containsExactly(PACK_MANAGE, AI_RESPONSE_MANAGE);
    }

    @Test
    void convertToEntityAttribute_WhenColumnNullOrBlank_ThenNoPermission() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
    }
}
