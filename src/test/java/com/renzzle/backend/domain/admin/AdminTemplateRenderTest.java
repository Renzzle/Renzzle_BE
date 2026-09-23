package com.renzzle.backend.domain.admin;

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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders every admin Thymeleaf template outside of Spring MVC.
 * Guards the shared fragments (siteNav / uiKit / languageSelect) against signature drift:
 * a caller passing the wrong arguments to a fragment only fails at render time.
 */
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

    /**
     * Superset of the model attributes AdminController / PuzzleCacheController put on their pages.
     */
    private static WebContext webContext() {
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        IWebExchange exchange = application.buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse());

        Map<String, Object> model = new HashMap<>();
        model.put("userEmail", "admin@test.com");
        model.put("langCodeNames", LangCode.LangCodeName.values());
        model.put("packId", 1L);
        model.put("puzzleId", 2L);
        model.put("puzzleType", "TRAINING");
        return new WebContext(exchange, Locale.ENGLISH, model);
    }

    private static String render(String template) {
        return engine.process(template, webContext());
    }

    /** Unprocessed Thymeleaf attribute left in the output, e.g. {@code th:text="..."}. */
    private static final Pattern LEFTOVER_TH_ATTRIBUTE = Pattern.compile("\\sth:[a-z-]+=");

    private static final Pattern SITE_NAV = Pattern.compile("<nav class=\"site-nav-wrap\".*?</nav>", Pattern.DOTALL);

    /** The main-menu anchor for {@code href}; header buttons may link to the same path, so search only inside the nav. */
    private static String navAnchor(String html, String href) {
        Matcher nav = SITE_NAV.matcher(html);
        assertThat(nav.find()).as("site nav present").isTrue();
        Matcher matcher = Pattern.compile("<a[^>]*href=\"" + Pattern.quote(href) + "\"[^>]*>").matcher(nav.group());
        assertThat(matcher.find()).as("nav anchor with href %s", href).isTrue();
        return matcher.group();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "admin/community-puzzles",
            "admin/error",
            "admin/login",
            "admin/notices",
            "admin/pack-create",
            "admin/pack-detail",
            "admin/pack-list",
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
        assertThat(navAnchor(html, "/puzzle-cache")).doesNotContain("active");
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
            assertThat(navAnchor(render(template), "/puzzle-cache")).as(template).contains("class=\"active\"");
        }
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
