package com.yourname.simpletranslate.gui;

import com.yourname.simpletranslate.SimpleTranslateMod;
import com.yourname.simpletranslate.config.ModConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Online provider selection with a real connectivity probe, without model setup. */
public final class OnlineTranslationScreen extends ScrollableSettingsScreen {
    public OnlineTranslationScreen(Screen parent) {
        super(Component.translatable("screen.simple_translate.online.title"), parent);
        this.contentWidth = 320;
    }

    @Override
    protected void buildContent() {
        addEntry(CycleButton.<ModConfig.TranslationProvider>builder(
                provider -> Component.translatable(provider == ModConfig.TranslationProvider.GOOGLE_WEB
                        ? "screen.simple_translate.online.google" : "screen.simple_translate.online.ai"),
                ModConfig.TRANSLATION_PROVIDER.get())
                .withValues(ModConfig.TranslationProvider.values())
                .create(0, 0, this.contentWidth, 20,
                        Component.translatable("screen.simple_translate.online.provider"), (button, provider) -> {
                            ModConfig.TRANSLATION_PROVIDER.set(provider);
                            SimpleTranslateMod.onGlobalTranslationSettingChanged(ModConfig.GLOBAL_ENABLED.get());
                            ModConfig.save();
                        }));
        addDescription(Component.translatable("screen.simple_translate.online.no_key").getString());
        addDescription(Component.translatable("screen.simple_translate.online.privacy").getString());
        addDescription(Component.translatable("screen.simple_translate.online.limits").getString());
        addEntry(Button.builder(Component.translatable("screen.simple_translate.online.test"), button -> {
            button.active = false;
            button.setMessage(Component.translatable("screen.simple_translate.online.testing"));
            long started = System.nanoTime();
            SimpleTranslateMod.getTranslationManager().detectApi().whenComplete((result, error) -> {
                this.minecraft.execute(() -> {
                    button.active = true;
                    button.setMessage(error == null && result != null && result.success()
                            ? Component.translatable("screen.simple_translate.online.ok",
                                    (System.nanoTime() - started) / 1_000_000L)
                            : Component.translatable("screen.simple_translate.online.failed",
                                    result == null ? "Connection error" : result.message()));
                });
            });
        }).bounds(0, 0, this.contentWidth, 20).build());
        addEntry(Button.builder(Component.translatable("screen.simple_translate.online.ai_settings"),
                button -> this.minecraft.gui.setScreen(new ModelSettingsScreen(this)))
                .bounds(0, 0, this.contentWidth, 20).build());
    }

    @Override
    protected void saveSettings() { ModConfig.save(); }
}
