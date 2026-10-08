package com.yourname.simpletranslate.transport;

import com.yourname.simpletranslate.config.ModConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebProviderSelectionTest {
    @Test
    void googleNeedsNoKeyAndHasDifferentCacheIdentityFromModelApi() {
        var previous = ModConfig.TRANSLATION_PROVIDER.get();
        String previousKey = ModConfig.DEEPSEEK_API_KEY.get();
        TranslationManager manager = new TranslationManager();
        try {
            ModConfig.DEEPSEEK_API_KEY.set("");
            ModConfig.TRANSLATION_PROVIDER.set(ModConfig.TranslationProvider.GOOGLE_WEB);
            assertTrue(manager.isReady());
            assertTrue(TranslationManager.usesGoogle());
            String webIdentity = TranslationPromptPolicy.cacheFingerprint("hud.actionbar.wynn.dialogue.content.paragraph.v5");
            ModConfig.TRANSLATION_PROVIDER.set(ModConfig.TranslationProvider.AI_API);
            assertFalse(manager.isReady());
            assertNotEquals(webIdentity, TranslationPromptPolicy.cacheFingerprint(
                    "hud.actionbar.wynn.dialogue.content.paragraph.v5"));
            assertFalse(TranslationPromptPolicy.legacyCacheCompatible());
        } finally {
            ModConfig.TRANSLATION_PROVIDER.set(previous);
            ModConfig.DEEPSEEK_API_KEY.set(previousKey);
        }
    }
}
