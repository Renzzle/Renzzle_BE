package com.renzzle.backend.domain.puzzle.community.web;

import com.renzzle.backend.domain.puzzle.community.api.response.GetSharedPuzzleResponse;
import com.renzzle.backend.global.common.domain.LangCode.LangCodeName;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

// The share page's words in the visitor's language, from messages/share_*.properties. Fields a missing puzzle
// doesn't show are null
public record SharedPuzzlePageText(
        String lang,
        String title,
        String description,
        String goal,
        String authorPrefix,
        String authorSuffix,
        String meta,
        String counts,
        String likes,
        String board,
        String retry,
        String bannerText,
        String bannerButton,
        String more,
        String rotate
) {

    private static final String BUNDLE = "messages/share";
    private static final ResourceBundle.Control NO_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);
    private static final String AUTHOR = "{author}";
    // Chinese in Taiwan, Hong Kong and Macau is written in Traditional characters
    private static final Set<String> TRADITIONAL_CHINESE = Set.of("hant", "tw", "hk", "mo");

    // The language the share link names with ?lang=, or null if it names none the page has
    public static LangCodeName languageOfParam(String lang) {
        return lang == null ? null : languageOfTag(lang);
    }

    // The visitor's first language the page has, or null if none. Link previews often send no header
    public static LangCodeName languageOf(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return null;
        }
        try {
            for (Locale.LanguageRange range : Locale.LanguageRange.parse(acceptLanguage)) {
                LangCodeName language = range.getWeight() > 0 ? languageOfTag(range.getRange()) : null;
                if (language != null) {
                    return language;
                }
            }
        } catch (IllegalArgumentException e) {
            // A malformed header is read as none of the page's languages
        }
        return null;
    }

    private static LangCodeName languageOfTag(String tag) {
        List<String> subtags = List.of(tag.toLowerCase(Locale.ROOT).split("-"));
        return switch (subtags.get(0)) {
            case "ko" -> LangCodeName.KO;
            case "en" -> LangCodeName.EN;
            case "ja" -> LangCodeName.JA;
            case "ru" -> LangCodeName.RU;
            case "et" -> LangCodeName.ET;
            case "zh" -> subtags.stream().anyMatch(TRADITIONAL_CHINESE::contains) ? LangCodeName.ZH_HANT : LangCodeName.ZH_HANS;
            default -> null;
        };
    }

    // The page's lang tag for a language, like zh-Hant
    public static String tagOf(LangCodeName language) {
        return Locale.forLanguageTag(language.name().replace('_', '-')).toLanguageTag();
    }

    public static SharedPuzzlePageText of(LangCodeName language, GetSharedPuzzleResponse puzzle) {
        String lang = tagOf(language);
        ResourceBundle words = ResourceBundle.getBundle(BUNDLE, Locale.forLanguageTag(lang),
                SharedPuzzlePageText.class.getClassLoader(), NO_FALLBACK);
        String bannerText = words.getString("bannerText");
        String bannerButton = words.getString("bannerButton");
        String rotate = words.getString("rotate");
        if (puzzle == null) {
            return new SharedPuzzlePageText(lang, words.getString("notFoundTitle"), words.getString("notFoundDescription"),
                    null, null, null, null, null, null, null, null, bannerText, bannerButton, null, rotate);
        }

        boolean black = "BLACK".equals(puzzle.winColor());
        String depth = words.getString("depth").replace("{depth}", String.valueOf(puzzle.depth()));
        // The name goes between the two parts, as it is set in bold
        String author = words.getString("author");
        String authorPrefix = author.substring(0, author.indexOf(AUTHOR));
        String authorSuffix = author.substring(author.indexOf(AUTHOR) + AUTHOR.length());
        String description = puzzle.description() != null && !puzzle.description().isBlank()
                ? puzzle.description()
                : String.join(" · ", words.getString(black ? "blackToWin" : "whiteToWin"), depth, words.getString("solveOnRenzzle"));

        return new SharedPuzzlePageText(
                lang,
                words.getString("title").replace(AUTHOR, puzzle.authorName()),
                description,
                words.getString(black ? "goalBlack" : "goalWhite"),
                authorPrefix,
                authorSuffix,
                " · " + depth + " · " + words.getString(puzzle.isVerified() ? "verified" : "unverified"),
                words.getString("counts")
                        .replace("{solved}", String.valueOf(puzzle.solvedCount()))
                        .replace("{views}", String.valueOf(puzzle.views())),
                words.getString("likes").replace("{likes}", String.valueOf(puzzle.likeCount())),
                words.getString("board"),
                words.getString("retry"),
                bannerText,
                bannerButton,
                words.getString("more"),
                rotate
        );
    }
}
