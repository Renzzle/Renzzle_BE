package com.renzzle.backend.global.exception;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void handleException_WhenIllegalArgumentEscapes_ThenAnswersServerError() throws Exception {
        // request values are validated before parsing, so this is a server bug, not a bad request
        mockMvc.perform(get("/illegal-argument"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorResponse.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    @RestController
    static class FailingController {

        @GetMapping("/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("Invalid win color name: null");
        }
    }
}
