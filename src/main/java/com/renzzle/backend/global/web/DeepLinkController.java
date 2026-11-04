package com.renzzle.backend.global.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// The files iOS and Android read to open renzzle.com share links in the app. Harmless before the app declares the domain
@RestController
public class DeepLinkController {

    private final String path;
    private final String appleAppId;
    private final String androidPackage;
    private final List<String> androidCertSha256;

    public DeepLinkController(
            @Value("${app.deep-link.path}") String path,
            @Value("${app.deep-link.apple-app-id:}") String appleAppId,
            @Value("${app.deep-link.android-package:}") String androidPackage,
            @Value("${app.deep-link.android-cert-sha256:}") String androidCertSha256
    ) {
        this.path = path;
        this.appleAppId = appleAppId;
        this.androidPackage = androidPackage;
        this.androidCertSha256 = Arrays.stream(androidCertSha256.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    @GetMapping(value = "/.well-known/apple-app-site-association", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> appleAppSiteAssociation() {
        if (!StringUtils.hasText(appleAppId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("applinks", Map.of("details", List.of(Map.of(
                "appIDs", List.of(appleAppId),
                "components", List.of(Map.of("/", path))
        )))));
    }

    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> assetLinks() {
        if (!StringUtils.hasText(androidPackage) || androidCertSha256.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(List.of(Map.of(
                "relation", List.of("delegate_permission/common.handle_all_urls"),
                "target", Map.of(
                        "namespace", "android_app",
                        "package_name", androidPackage,
                        "sha256_cert_fingerprints", androidCertSha256
                )
        )));
    }
}
