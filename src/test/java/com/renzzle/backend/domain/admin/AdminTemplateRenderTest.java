package com.renzzle.backend.domain.admin;

import com.renzzle.backend.domain.auth.domain.AdminPermission;
import com.renzzle.backend.global.common.domain.LangCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// Renders every admin template; a wrong fragment call only fails at render time
class AdminTemplateRenderTest {

    private static SpringTemplateEngine engine;

    @BeforeAll
    static void setUpEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
    }

    private static final List<String> ALL_PERMISSIONS = Arrays.stream(AdminPermission.values()).map(Enum::name).toList();

    // Superset of the model attributes the admin pages use
    private static WebContext webContext(Set<String> adminPermissions) {
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        IWebExchange exchange = application.buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse());

        Map<String, Object> model = new HashMap<>();
        model.put("userEmail", "admin@test.com");
        model.put("langCodeNames", LangCode.LangCodeName.values());
        model.put("packId", 1L);
        model.put("puzzleId", 2L);
        model.put("puzzleType", "TRAINING");
        model.put("adminPermissions", adminPermissions);
        model.put("permissionNames", ALL_PERMISSIONS);
        return new WebContext(exchange, Locale.ENGLISH, model);
    }

    private static String render(String template) {
        return render(template, Set.copyOf(ALL_PERMISSIONS));
    }

    private static String render(String template, Set<String> adminPermissions) {
        return engine.process(template, webContext(adminPermissions));
    }

    // Unprocessed th: attribute left in the output
    private static final Pattern LEFTOVER_TH_ATTRIBUTE = Pattern.compile("\\sth:[a-z-]+=");

    private static final Pattern SITE_NAV = Pattern.compile("<nav class=\"site-nav-wrap\".*?</nav>", Pattern.DOTALL);

    private static String nav(String html) {
        Matcher nav = SITE_NAV.matcher(html);
        assertThat(nav.find()).as("site nav present").isTrue();
        return nav.group();
    }

    // Search only the nav; header buttons can share the href
    private static String navAnchor(String html, String href) {
        Matcher matcher = Pattern.compile("<a[^>]*href=\"" + Pattern.quote(href) + "\"[^>]*>").matcher(nav(html));
        assertThat(matcher.find()).as("nav anchor with href %s", href).isTrue();
        return matcher.group();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "admin/admin-accounts",
            "admin/community-puzzles",
            "admin/error",
            "admin/forbidden",
            "admin/login",
            "admin/notices",
            "admin/pack-create",
            "admin/pack-detail",
            "admin/pack-list",
            "admin/payments",
            "admin/puzzle-add",
            "admin/puzzle-cache",
            "admin/puzzle-cache-board",
            "admin/puzzle-cache-training-pack",
            "admin/puzzle-edit"
    })
    void adminTemplate_RendersWithoutError(String template) {
        String html = render(template);

        assertThat(html).startsWith("<!DOCTYPE html>");
        assertThat(LEFTOVER_TH_ATTRIBUTE.matcher(html).find()).as("no unprocessed th:* attribute").isFalse();
    }

    @Test
    void siteNav_ContainsNoticesLinkOnEveryPageThatUsesIt() {
        for (String template : new String[]{"admin/pack-list", "admin/community-puzzles", "admin/puzzle-cache", "admin/notices"}) {
            String html = render(template);
            assertThat(html).as(template).contains("href=\"/admin/notices\"");
            assertThat(html).as(template).contains("data-i18n=\"nav.notices\"");
        }
    }

    @Test
    void notices_MarksOnlyNoticesTabActive() {
        String html = render("admin/notices");

        assertThat(navAnchor(html, "/admin/notices")).contains("class=\"active\"");
        assertThat(navAnchor(html, "/admin/pack-list")).doesNotContain("active");
        assertThat(navAnchor(html, "/admin/community-puzzles")).doesNotContain("active");
        assertThat(navAnchor(html, "/admin/puzzle-cache")).doesNotContain("active");
    }

    @Test
    void packPages_MarkOnlyPacksTabActive() {
        for (String template : new String[]{"admin/pack-list", "admin/pack-create", "admin/pack-detail", "admin/puzzle-add", "admin/puzzle-edit"}) {
            String html = render(template);
            assertThat(navAnchor(html, "/admin/pack-list")).as(template).contains("class=\"active\"");
            assertThat(navAnchor(html, "/admin/notices")).as(template).doesNotContain("active");
        }
    }

    @Test
    void communityAndCachePages_MarkTheirOwnTabActive() {
        assertThat(navAnchor(render("admin/community-puzzles"), "/admin/community-puzzles")).contains("class=\"active\"");
        for (String template : new String[]{"admin/puzzle-cache", "admin/puzzle-cache-board", "admin/puzzle-cache-training-pack"}) {
            assertThat(navAnchor(render(template), "/admin/puzzle-cache")).as(template).contains("class=\"active\"");
        }
    }

    @Test
    void payments_MarksOnlyPaymentsTabActiveAndEveryNavLinksToIt() {
        String html = render("admin/payments");

        assertThat(navAnchor(html, "/admin/payments")).contains("class=\"active\"");
        assertThat(navAnchor(html, "/admin/notices")).doesNotContain("active");
        assertThat(navAnchor(html, "/admin/pack-list")).doesNotContain("active");
        assertThat(html).contains("admin@test.com");
        for (String template : new String[]{"admin/pack-list", "admin/community-puzzles", "admin/puzzle-cache", "admin/notices"}) {
            assertThat(navAnchor(render(template), "/admin/payments")).as(template).doesNotContain("active");
        }
    }

    @Test
    void siteNav_ShowsOnlyTabsTheAdminHasPermissionFor() {
        String packOnly = nav(render("admin/pack-list", Set.of(AdminPermission.PACK_MANAGE.name())));
        assertThat(packOnly).contains("href=\"/admin/pack-list\"")
                .doesNotContain("/admin/community-puzzles", "/admin/notices", "/admin/puzzle-cache", "/admin/payments", "/admin/admin-accounts");

        String operationOnly = nav(render("admin/notices", Set.of(AdminPermission.OPERATION_MANAGE.name())));
        assertThat(operationOnly).contains("href=\"/admin/notices\"", "href=\"/admin/payments\"", "href=\"/admin/admin-accounts\"")
                .doesNotContain("/admin/pack-list", "/admin/community-puzzles", "/admin/puzzle-cache");
    }

    @Test
    void adminAccounts_RendersOnePermissionOptionAndColumnPerPermission() {
        String html = render("admin/admin-accounts");

        assertThat(navAnchor(html, "/admin/admin-accounts")).contains("class=\"active\"");
        for (AdminPermission permission : AdminPermission.values()) {
            assertThat(html).contains("value=\"" + permission.name() + "\"");
            assertThat(html).contains("data-i18n=\"permission." + permission.name() + "\"");
        }
    }

    @Test
    void forbidden_OffersAnAvailableMenuOnlyWhenTheAdminHasOne() {
        assertThat(render("admin/forbidden")).contains("href=\"/admin/dashboard\"").doesNotContain("forbidden.noPermissions");

        assertThat(render("admin/forbidden", Set.of())).doesNotContain("href=\"/admin/dashboard\"").contains("forbidden.noPermissions");
    }

    // Scripts read these instead of inlined expressions, which need th:inline to be replaced
    @Test
    void pageScripts_GetServerValuesFromBodyDataAttributes() {
        String langCodes = String.join(",", Arrays.stream(LangCode.LangCodeName.values()).map(Enum::name).toList());

        assertThat(render("admin/pack-detail")).contains("<body data-pack-id=\"1\">");
        assertThat(render("admin/puzzle-add")).contains("<body data-pack-id=\"1\">");
        assertThat(render("admin/puzzle-cache-training-pack")).contains("<body data-pack-id=\"1\">");
        assertThat(render("admin/puzzle-edit")).contains("<body data-pack-id=\"1\" data-puzzle-id=\"2\">");
        assertThat(render("admin/puzzle-cache-board")).contains("<body data-puzzle-type=\"TRAINING\" data-puzzle-id=\"2\">");
        assertThat(render("admin/pack-create")).contains("<body data-lang-codes=\"" + langCodes + "\">");
        assertThat(render("admin/admin-accounts")).contains("<body data-permissions=\"" + String.join(",", ALL_PERMISSIONS) + "\">");
    }

    @Test
    void notices_RendersOneCreateCardAndOneFilterOptionPerLanguage() {
        String html = render("admin/notices");

        for (LangCode.LangCodeName lang : LangCode.LangCodeName.values()) {
            assertThat(html).contains("data-lang-code=\"" + lang.name() + "\"");
            assertThat(html).contains("<option value=\"" + lang.name() + "\">" + lang.name() + "</option>");
        }
        assertThat(html).contains("admin@test.com");
        assertThat(html).contains("id=\"adminModalDialog\"");
    }

}
