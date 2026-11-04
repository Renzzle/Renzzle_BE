package com.renzzle.backend.global.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeepLinkControllerTest {

    private static final String PATH = "/puzzle/*";

    @Test
    void appleAppSiteAssociation_WhenAppIdSet_ThenListsTheSharePathForTheApp() {
        DeepLinkController controller = new DeepLinkController(PATH, "TEAM123.com.renzzle", "com.renzzle_fe", "");

        Map<String, Object> body = controller.appleAppSiteAssociation().getBody();

        assertThat(body).isEqualTo(Map.of("applinks", Map.of("details", List.of(Map.of(
                "appIDs", List.of("TEAM123.com.renzzle"),
                "components", List.of(Map.of("/", PATH)))))));
    }

    @Test
    void appleAppSiteAssociation_WhenAppIdBlank_ThenNotFound() {
        DeepLinkController controller = new DeepLinkController(PATH, " ", "com.renzzle_fe", "");

        assertThat(controller.appleAppSiteAssociation().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void assetLinks_WhenFingerprintsSet_ThenTrustsEachForThePackage() {
        DeepLinkController controller = new DeepLinkController(PATH, "TEAM123.com.renzzle", "com.renzzle_fe", "AA:BB, CC:DD ,");

        List<Map<String, Object>> body = controller.assetLinks().getBody();

        assertThat(body).isEqualTo(List.of(Map.of(
                "relation", List.of("delegate_permission/common.handle_all_urls"),
                "target", Map.of(
                        "namespace", "android_app",
                        "package_name", "com.renzzle_fe",
                        "sha256_cert_fingerprints", List.of("AA:BB", "CC:DD")))));
    }

    // Until the Play Console fingerprint is configured, Android finds no file instead of one that trusts nothing
    @Test
    void assetLinks_WhenNoFingerprint_ThenNotFound() {
        DeepLinkController controller = new DeepLinkController(PATH, "TEAM123.com.renzzle", "com.renzzle_fe", "");

        assertThat(controller.assetLinks().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
