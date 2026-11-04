package com.renzzle.backend.global.web;

import com.renzzle.backend.config.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.class)
class DeepLinkIntegrationTest {

    @Autowired private MockMvc mockMvc;

    // Apple's CDN fetches it with no login or app key
    @Test
    void appleAppSiteAssociation_WithoutLoginOrAppKey_ThenServedAsJson() throws Exception {
        mockMvc.perform(get("/.well-known/apple-app-site-association"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.applinks.details[0].appIDs[0]").value("XTFB5FCGMJ.com.renzzle"))
                .andExpect(jsonPath("$.applinks.details[0].components[0]['/']").value("/puzzle/*"));
    }

    // A 404 until the fingerprint is set; either way security lets Android's request through
    @Test
    void assetLinks_WithoutLoginOrAppKey_ThenNotBlocked() throws Exception {
        mockMvc.perform(get("/.well-known/assetlinks.json"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(200, 404));
    }
}
