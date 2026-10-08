package com.yourname.simpletranslate.transport;

import org.junit.jupiter.api.Test;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GoogleWebTranslationServiceTest {
    @Test
    void parsesRealBrowserRpcFramesWithoutReturningSourceOrSpellingSuggestions() throws Exception {
        try (var input = getClass().getResourceAsStream("/google-web-rpc-batch.txt")) {
            assertNotNull(input);
            String translated = GoogleWebTranslationService.parseWebRpcResponse(
                    new String(input.readAllBytes(), StandardCharsets.UTF_8));
            assertEquals("Как мне прокормить свою семью?\n[91827364509182736450]\n"
                    + "Мне очень нужно Craftmas чудо...\n[91827364509182736450]\nКак мне платить за хлеб?", translated);
        }
        assertThrows(RuntimeException.class, () -> GoogleWebTranslationService.parseWebRpcResponse(
                ")]}'\n\n103\n[[\"wrb.fr\",\"MkEWBc\",null,null,null,[3],\"generic\"]]\n"));
    }

    @Test
    void browserPostPreservesQuotesBackslashesNewlinesAndUnicodeInNestedJson() {
        String source = "He says: \"Hello!\"\nA path C:\\notes and a traveller 😀";
        String body = GoogleWebTranslationService.webRpcBody(source, "auto", "ru");
        var envelope = com.google.gson.JsonParser.parseString(java.net.URLDecoder.decode(
                body.substring("f.req=".length()), StandardCharsets.UTF_8)).getAsJsonArray();
        var call = envelope.get(0).getAsJsonArray().get(0).getAsJsonArray();
        assertEquals("MkEWBc", call.get(0).getAsString());
        var parameters = com.google.gson.JsonParser.parseString(call.get(1).getAsString()).getAsJsonArray();
        assertEquals(source, parameters.get(0).getAsJsonArray().get(0).getAsString());
        assertEquals("auto", parameters.get(0).getAsJsonArray().get(1).getAsString());
        assertEquals("ru", parameters.get(0).getAsJsonArray().get(2).getAsString());
    }

    @Test
    void joinsGoogleSentencesInOrderAndRejectsMalformedResponses() {
        assertEquals("Привет! Начни путь.", GoogleWebTranslationService.parseResponse(
                "[[[\"Привет! \",\"Hello! \"],[\"Начни путь.\",\"Begin your journey.\"]],null,\"en\"]"));
        assertThrows(RuntimeException.class, () -> GoogleWebTranslationService.parseResponse("<html>error</html>"));
        assertThrows(RuntimeException.class, () -> GoogleWebTranslationService.parseResponse("[[]]"));
    }

    @Test
    void mapsMinecraftLocalesAndDoesNotAllowAutoAsTarget() {
        assertEquals("ru", GoogleWebTranslationService.googleLanguage("ru_RU", false));
        assertEquals("en", GoogleWebTranslationService.googleLanguage("en_us", true));
        assertEquals("zh-CN", GoogleWebTranslationService.googleLanguage("zh_cn", false));
        assertEquals("zh-TW", GoogleWebTranslationService.googleLanguage("zh_hant", false));
        assertEquals("auto", GoogleWebTranslationService.googleLanguage("auto", true));
        assertThrows(IllegalArgumentException.class, () -> GoogleWebTranslationService.googleLanguage("auto", false));
        assertThrows(IllegalArgumentException.class, () -> GoogleWebTranslationService.googleLanguage("ru&tl=en", false));
    }

    @Test
    void batchingKeepsOrderAndIsolatesLiteralDelimiter() {
        String literal = "A literal " + GoogleWebTranslationService.BATCH_MARKER + " token";
        List<String> texts = List.of("Speak to the captain.", "Press SHIFT to continue.", literal, "Reward");
        List<List<String>> batches = GoogleWebTranslationService.batches(texts);
        assertEquals(texts, batches.stream().flatMap(List::stream).toList());
        assertEquals(List.of(literal), batches.get(1));
    }

    @Test
    void longUnicodeTextIsSplitWithoutLosingCharactersOrBreakingSurrogates() {
        String text = "Speak to the captain. " + "旅人😀Привет ".repeat(600);
        List<String> chunks = GoogleWebTranslationService.splitLongText(text);
        assertTrue(chunks.size() > 1);
        assertEquals(text, String.join("", chunks));
        for (String chunk : chunks) {
            assertTrue(URLEncoder.encode(chunk, StandardCharsets.UTF_8).length() <= 7000);
            assertFalse(Character.isHighSurrogate(chunk.charAt(chunk.length() - 1)));
            assertFalse(Character.isLowSurrogate(chunk.charAt(0)));
        }
    }

    @Test
    void plainAsciiIsAlsoSplitToRespectBrowserTextLimit() {
        String source = "x".repeat(9001);
        var chunks = GoogleWebTranslationService.splitLongText(source);
        assertEquals(source, String.join("", chunks));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 4500));
        assertEquals(3, chunks.size());
    }
}
