package com.yourname.simpletranslate.feature.wynn;

import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WynnNativeDialogueRendererTest {
    @Test
    void longRussianParagraphRewrapsBeforeShrinkingAndNeverClips() {
        var fit = WynnNativeDialogueRenderer.fit(232, 56, 10,
                width -> (int) Math.ceil(1800.0 / width));
        assertNotNull(fit);
        assertTrue(fit.scale() < 1);
        assertTrue(Math.ceil(1800.0 / fit.wrapWidth()) * 10 * fit.scale() <= 56.001);
        assertNull(WynnNativeDialogueRenderer.fit(232, 56, 10,
                width -> (int) Math.ceil(8000.0 / width)));
    }

    @Test
    void deferredMaskPreservesAllEventsAndOnlyHidesSelectedProse() {
        var events = List.of(
                new WynnDialogueProjection.GlyphEvent(0, 0, Style.EMPTY, "frame", 0xE000),
                new WynnDialogueProjection.GlyphEvent(1, 0, Style.EMPTY, "body", 'A'),
                new WynnDialogueProjection.GlyphEvent(2, 0, Style.EMPTY, "control", 0xE001));
        BitSet mask = new BitSet(); mask.set(1);
        var sequence = new WynnDialogueProjection.EventSequence(events, mask);
        mask.clear(); // Deferred drawing must own its mask snapshot.
        java.util.ArrayList<Integer> seen = new java.util.ArrayList<>();
        assertTrue(sequence.accept((index, style, cp) -> {
            seen.add(cp);
            assertEquals(cp == 'A', WynnActionbarGlyphOverlayPlan.isCurrentGlyphMasked());
            return true;
        }));
        assertEquals(List.of(0xE000, (int) 'A', 0xE001), seen);
        assertFalse(WynnActionbarGlyphOverlayPlan.isCurrentGlyphMasked());
        assertThrows(IllegalStateException.class, () -> sequence.accept((index, style, cp) -> {
            if (cp == 'A') throw new IllegalStateException();
            return true;
        }));
        assertFalse(WynnActionbarGlyphOverlayPlan.isCurrentGlyphMasked());
    }
}
