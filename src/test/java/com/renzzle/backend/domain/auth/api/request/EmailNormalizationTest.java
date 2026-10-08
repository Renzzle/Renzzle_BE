package com.renzzle.backend.domain.auth.api.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.global.util.EmailUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailNormalizationTest {

    private static final String RAW = "  Victim@Example.COM ";
    private static final String NORMALIZED = "victim@example.com";

    @Test
    void authRequests_WhenEmailHasUppercaseOrSpaces_ThenNormalizeIt() {
        assertThat(new AuthEmailRequest(RAW).email()).isEqualTo(NORMALIZED);
        assertThat(new ConfirmCodeRequest(RAW, "123456").email()).isEqualTo(NORMALIZED);
        assertThat(new LoginRequest(RAW, "password1").email()).isEqualTo(NORMALIZED);
        assertThat(new ResetPasswordRequest(RAW, "token", "password1").email()).isEqualTo(NORMALIZED);
        assertThat(new SignupRequest(RAW, "password1", "nick", "token", "device").email()).isEqualTo(NORMALIZED);
        assertThat(new TestTokenRequest(RAW, "password1").email()).isEqualTo(NORMALIZED);
    }

    @Test
    void loginRequest_WhenDeserializedFromJson_ThenNormalizesEmail() throws Exception {
        LoginRequest request = new ObjectMapper().readValue(
                "{\"email\":\"Victim@Example.COM\",\"password\":\"password1\"}", LoginRequest.class);

        assertThat(request.email()).isEqualTo(NORMALIZED);
    }

    @Test
    void normalize_WhenEmailMissing_ThenLeavesNullForValidation() {
        assertThat(EmailUtils.normalize(null)).isNull();
    }

}
