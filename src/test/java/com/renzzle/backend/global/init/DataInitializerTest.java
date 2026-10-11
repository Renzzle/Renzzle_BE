package com.renzzle.backend.global.init;

import com.renzzle.backend.domain.auth.dao.AdminRepository;
import com.renzzle.backend.domain.user.dao.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    private static final String WIDEN_STATUS_SQL = "ALTER TABLE in_app_purchase MODIFY status VARCHAR(20) NOT NULL";

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void widenInAppPurchaseStatusColumn_WhenColumnIsEnum_ThenChangesItToVarchar() {
        // given
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of("enum"));

        // when
        dataInitializer.widenInAppPurchaseStatusColumn();

        // then
        verify(jdbcTemplate).execute(WIDEN_STATUS_SQL);
    }

    @Test
    void widenInAppPurchaseStatusColumn_WhenColumnIsAlreadyVarchar_ThenDoesNothing() {
        // given
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of("varchar"));

        // when
        dataInitializer.widenInAppPurchaseStatusColumn();

        // then
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void widenInAppPurchaseStatusColumn_WhenTableDoesNotExist_ThenDoesNothing() {
        // given
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of());

        // when
        dataInitializer.widenInAppPurchaseStatusColumn();

        // then
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void widenInAppPurchaseStatusColumn_WhenAlterFails_ThenStartupContinues() {
        // given
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of("enum"));
        doThrow(new DataAccessResourceFailureException("Lock wait timeout exceeded"))
                .when(jdbcTemplate).execute(WIDEN_STATUS_SQL);

        // when, then
        assertDoesNotThrow(() -> dataInitializer.widenInAppPurchaseStatusColumn());
    }
}
