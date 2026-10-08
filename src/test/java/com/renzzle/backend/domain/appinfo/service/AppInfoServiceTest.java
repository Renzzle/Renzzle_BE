package com.renzzle.backend.domain.appinfo.service;

import com.renzzle.backend.domain.appinfo.dao.AppInfoRepository;
import com.renzzle.backend.domain.appinfo.domain.AppInfo;
import com.renzzle.backend.global.common.constant.ItemPrice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppInfoServiceTest {

    @Mock
    private AppInfoRepository appInfoRepository;

    @InjectMocks
    private AppInfoService appInfoService;

    @Test
    void getPrice_WhenRowExists_ThenReturnsStoredValue() {
        // Given
        when(appInfoRepository.findByTag("hint_price"))
                .thenReturn(Optional.of(AppInfo.builder().tag("hint_price").value(" 350 ").build()));

        // When
        int price = appInfoService.getPrice(ItemPrice.HINT);

        // Then
        assertThat(price).isEqualTo(350);
    }

    @Test
    void getPrice_WhenRowMissing_ThenFallsBackToDefault() {
        // Given
        when(appInfoRepository.findByTag("change_nickname_price")).thenReturn(Optional.empty());

        // When
        int price = appInfoService.getPrice(ItemPrice.CHANGE_NICKNAME);

        // Then
        assertThat(price).isEqualTo(ItemPrice.CHANGE_NICKNAME.getDefaultPrice());
    }

}
