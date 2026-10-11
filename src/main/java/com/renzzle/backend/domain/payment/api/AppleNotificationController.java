package com.renzzle.backend.domain.payment.api;

import com.renzzle.backend.domain.payment.api.request.AppleNotificationRequest;
import com.renzzle.backend.domain.payment.service.AppleNotificationService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Outside /api because Apple sends neither the app key nor a login token
@RestController
@RequestMapping("/webhooks/apple")
@RequiredArgsConstructor
@Tag(name = "Apple Notification API", description = "App Store Server Notifications V2 receiver")
public class AppleNotificationController {

    private final AppleNotificationService appleNotificationService;

    @Operation(summary = "Receive App Store server notification",
            description = "Called by Apple. Applies refunds and answers consumption requests")
    @PostMapping("/notifications")
    public ApiResponse<Object> receiveNotification(@Valid @RequestBody AppleNotificationRequest request) {
        appleNotificationService.handle(request.signedPayload());
        return ApiUtils.success(null);
    }
}
