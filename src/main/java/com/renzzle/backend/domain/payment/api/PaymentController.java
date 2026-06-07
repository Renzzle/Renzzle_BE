package com.renzzle.backend.domain.payment.api;

import com.renzzle.backend.domain.payment.api.request.VerifyInAppPurchaseRequest;
import com.renzzle.backend.domain.payment.api.response.VerifyInAppPurchaseResponse;
import com.renzzle.backend.domain.payment.service.PaymentService;
import com.renzzle.backend.global.common.response.ApiResponse;
import com.renzzle.backend.global.security.UserDetailsImpl;
import com.renzzle.backend.global.util.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment API", description = "Payment API")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Verify in-app purchase", description = "Verify Google Play or App Store purchase receipt")
    @PostMapping("/in-app/verify")
    public ApiResponse<VerifyInAppPurchaseResponse> verifyInAppPurchase(
            @Valid @RequestBody VerifyInAppPurchaseRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        VerifyInAppPurchaseResponse response = paymentService.verifyInAppPurchase(user.getUser(), request);
        return ApiUtils.success(response);
    }
}
