package com.renzzle.backend.domain.auth.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

// Stored as "PACK_MANAGE,OPERATION_MANAGE"; names no longer in the enum are dropped
@Converter
public class AdminPermissionsConverter implements AttributeConverter<Set<AdminPermission>, String> {

    private static final String SEPARATOR = ",";

    @Override
    public String convertToDatabaseColumn(Set<AdminPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "";
        }
        return EnumSet.copyOf(permissions).stream()
                .map(Enum::name)
                .collect(Collectors.joining(SEPARATOR));
    }

    @Override
    public Set<AdminPermission> convertToEntityAttribute(String column) {
        Set<AdminPermission> permissions = EnumSet.noneOf(AdminPermission.class);
        if (column == null || column.isBlank()) {
            return permissions;
        }
        Set<String> names = Arrays.stream(column.split(SEPARATOR)).map(String::trim).collect(Collectors.toSet());
        for (AdminPermission permission : AdminPermission.values()) {
            if (names.contains(permission.name())) {
                permissions.add(permission);
            }
        }
        return permissions;
    }
}
