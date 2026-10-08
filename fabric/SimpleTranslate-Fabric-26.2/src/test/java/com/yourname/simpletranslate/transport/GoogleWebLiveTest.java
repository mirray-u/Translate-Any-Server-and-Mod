package com.yourname.simpletranslate.transport;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.yourname.simpletranslate.api.TranslationRequest;
import com.yourname.simpletranslate.api.TranslationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "simpletranslate.liveGoogleTest", matches = "true")
class GoogleWebLiveTest {
    @Test
    void realWebTranslatorHandlesSeveralUncachedNpcRepliesInSequence() throws Exception {
        // Deliberately restrict this test to the browser RPC, independent of gtx.
        var service = new GoogleWebTranslationService(List.of(GoogleWebTranslationService.ENDPOINT));
        for (String source : List.of("How am I supposed to feed my family?",
                "I really need a Craftmas miracle...", "How am I supposed to pay for bread?",
                "Would you like to begin your journey?")) {
            long started = System.nanoTime();
            var translated = service.translateTexts(List.of(source), "auto", "ru",
                    "hud.actionbar.wynn.dialogue.content.paragraph.v5", List.of()).get(20, TimeUnit.SECONDS);
            assertEquals(1, translated.size());
            assertTrue(translated.getFirst().matches("(?s).*[А-Яа-яЁё].*"));
            assertNotEquals(source, translated.getFirst());
            System.out.println("LIVE_SEQUENCE durationMs=" + (System.nanoTime() - started) / 1_000_000
                    + " translation=" + translated);
        }
    }

    @Test
    void realGoogleTranslatesDialogueBatchAndReusesMemory() throws Exception {
        GoogleWebTranslationService service = new GoogleWebTranslationService();
        JsonArray source = new JsonArray();
        source.add("Welcome to Wynncraft! Speak to the captain to begin your journey.");
        source.add("Press SHIFT to continue.");
        source.add("Reward: 32 emeralds");
        source.add("How am I supposed to pay for bread?");
        TranslationRequest request = new TranslationRequest("hud.actionbar.wynn.dialogue.content.paragraph.v5",
                List.of(source.toString()), List.of(), 1, "en", "ru");
        long started = System.nanoTime();
        TranslationResult result = service.translate(request).get(30, TimeUnit.SECONDS);
        long coldMs = (System.nanoTime() - started) / 1_000_000;
        assertInstanceOf(TranslationResult.Success.class, result, result.toString());
        JsonArray translated = JsonParser.parseString(((TranslationResult.Success) result).payload()).getAsJsonArray();
        assertEquals(4, translated.size());
        for (var text : translated) assertTrue(text.getAsString().matches("(?s).*[А-Яа-яЁё].*"), text.toString());
        assertTrue(translated.get(2).getAsString().contains("32"));
        assertTrue(translated.get(3).getAsString().length() < 150, "A fresh NPC translation must not reuse a corrupted AI paragraph");
        started = System.nanoTime();
        assertEquals(result, service.translate(request).get(1, TimeUnit.SECONDS));
        long warmMs = (System.nanoTime() - started) / 1_000_000;
        assertTrue(warmMs < 500);
        System.out.println("LIVE_GOOGLE coldMs=" + coldMs + " warmMs=" + warmMs + " translation=" + translated);
    }
}
