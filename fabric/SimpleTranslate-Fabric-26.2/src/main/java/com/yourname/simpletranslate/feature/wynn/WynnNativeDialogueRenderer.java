package com.yourname.simpletranslate.feature.wynn;

import com.yourname.simpletranslate.core.ActiveFontManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Wynn's dialogue fonts have zero net stream width and shader-controlled pixels.
 * Keep the entire original pen stream, masking only verified prose callbacks.
 * Draw replacements in the 26.2 pack's native regions, independently of bitmap
 * bounds. Names, portraits, frame, arrows, keycaps and hidden choices stay local.
 */
public final class WynnNativeDialogueRenderer {
    private static final FontDescription FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("simple_translate", "dialogue"));
    private static Cache cached;

    private WynnNativeDialogueRenderer() { }

    public static boolean render(GuiGraphicsExtractor graphics, Font font,
                                 WynnDialogueProjection projection, WynnDialogueRenderPlan plan,
                                 int x, int y, int width, int color) {
        if (graphics == null || font == null || projection == null || plan == null) return false;
        long revision = ActiveFontManager.resourceRevision();
        Cache current = cached;
        if (current == null || current.plan != plan || current.font != font || current.revision != revision) {
            current = build(font, projection, plan, revision);
            cached = current;
        }
        if (current.mask.isEmpty()) return false;
        graphics.textWithBackdrop(font, Component.empty(), x, y, width, color);
        // The mask is captured by the deferred sequence itself, not a render-wide
        // flag. Font still receives every source glyph and advances its pen.
        graphics.text(font, new WynnDialogueProjection.EventSequence(projection.events(), current.mask),
                x, y, color, true);
        for (Panel panel : current.panels) {
            graphics.pose().pushMatrix();
            try {
                // The actionbar pose already supplies the screen center and H-68.
                graphics.pose().translate(panel.left, panel.top);
                graphics.pose().scale(panel.scale, panel.scale);
                for (int i = 0; i < panel.lines.size(); i++) {
                    graphics.text(font, panel.lines.get(i), 0, i * (font.lineHeight + 1), color, true);
                }
            } finally {
                graphics.pose().popMatrix();
            }
        }
        return true;
    }

    private static Cache build(Font font, WynnDialogueProjection projection,
                               WynnDialogueRenderPlan plan, long revision) {
        List<Panel> panels = new ArrayList<>();
        BitSet mask = new BitSet(projection.events().size());
        for (WynnDialogueRenderPlan.TranslatedSlot slot : plan.translatedSlots()) {
            if (!slot.sourceVisible()) continue;
            WynnDialogueProjection.SemanticKind kind = slot.source().kind();
            // Preserve the server's nameplate and complete control prompt,
            // including the SHIFT keycap and "to continue" at its native anchor.
            if (kind == WynnDialogueProjection.SemanticKind.NAME
                    || kind == WynnDialogueProjection.SemanticKind.CONTROL) continue;
            Component styled = restyle(slot.component());
            Region region = region(slot.source());
            if (region == null) continue;
            Fit fit = fit(region.width, region.height, font.lineHeight + 1,
                    budget -> font.split(styled, budget).size());
            if (fit == null) continue; // Atomic fallback: never clip or erase a paragraph.
            List<FormattedCharSequence> lines = font.split(styled, fit.wrapWidth);
            if (lines.isEmpty()) continue;
            List<Integer> ordinals = kind == WynnDialogueProjection.SemanticKind.BODY
                    ? slot.bodyMaskOrdinals() : slot.source().regions().stream()
                    .flatMap(row -> row.maskOrdinals().stream()).toList();
            if (ordinals.isEmpty()) continue;
            for (int ordinal : ordinals) mask.set(ordinal);
            panels.add(new Panel(region.left, region.top, fit.scale, List.copyOf(lines)));
        }
        return new Cache(plan, font, revision, mask, List.copyOf(panels));
    }

    private static Component restyle(Component source) {
        var result = Component.empty();
        source.visit((style, text) -> {
            result.append(Component.literal(text).withStyle(style.withFont(FONT)));
            return java.util.Optional.empty();
        }, source.getStyle());
        return result;
    }

    /** Coordinates are GUI units relative to vanilla's actionbar pose, not pixels. */
    static Region region(WynnDialogueProjection.SemanticSlot slot) {
        return switch (slot.kind()) {
            // Native first-row text begins at H-99. The actionbar pose is H-68;
            // bitmap texture padding must not be added to the replacement font.
            case BODY -> new Region(-116, -31, 232, 56);
            case OPTION -> {
                if (slot.regions().isEmpty()) yield null;
                int row = slot.regions().getFirst().line().ordinal()
                        - WynnDialogueProjection.DialogueLine.CHOICE_0.ordinal();
                yield row < 0 || row > 3 ? null : new Region(-104, -91 + row * 13, 220, 11);
            }
            case NAME, CONTROL -> null;
        };
    }

    /** Rewrap before shrinking, preserving complete text. No ellipsis or truncation. */
    static Fit fit(int width, int height, int lineHeight, IntUnaryOperator lineCount) {
        if (width <= 0 || height <= 0 || lineHeight <= 0) return null;
        for (int step = 20; step >= 12; step--) {
            float scale = step / 20.0F;
            int budget = (int) Math.floor(width / scale);
            int count = lineCount.applyAsInt(budget);
            if (count > 0 && count * lineHeight * scale <= height) return new Fit(scale, budget);
        }
        return null;
    }

    record Region(int left, int top, int width, int height) { }
    record Fit(float scale, int wrapWidth) { }
    private record Panel(int left, int top, float scale, List<FormattedCharSequence> lines) { }
    private record Cache(WynnDialogueRenderPlan plan, Font font, long revision,
                         BitSet mask, List<Panel> panels) { }
}
