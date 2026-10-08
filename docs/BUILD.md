# Simple Translate Web - Minecraft 26.2 / Fabric

A modified version of SimpleTranslate by baokaixina, adding keyless Google web
translation and translated dialogue inside Wynncraft's native dialogue frame.
Original project: https://github.com/baokaixina/SimpleTranslate
License: MIT. The original copyright notice is preserved in LICENSE.

## Install

Use Minecraft Java Edition 26.2, Java 25, Fabric Loader and Fabric API for 26.2.
Tested with Fabric Loader 0.19.5 and Fabric API 0.155.2+26.2.
Remove any other SimpleTranslate JAR before installing this JAR in mods.
Mod Menu is optional and provides access to the settings screen.
Google is the default translation provider; the default target language is Russian.
Choose source and target languages in the language settings. The source can be auto.
The settings shortcut is configurable; GUI translation uses K when enabled.

## Translation

Supported features include chat, item tooltips, books, signs, HUD, entity names,
text displays and supported mod interfaces. Coverage depends on how text is drawn.
Wynncraft dialogue translation preserves the native frame, NPC label and controls.
The original remains visible until translation is ready. Long translated prose is
wrapped and reduced in size down to 60%; if it still does not fit, the original stays.
Unusual interfaces on other servers may need separate integration.
Text embedded in images or textures is not translated.
Language detection has limitations: Latin text can be skipped when targeting English,
and other Cyrillic languages can be skipped when targeting Russian.

## Privacy and cache

This download contains no player configuration, translation cache, chat logs,
Minecraft account credentials or API keys.
During use, text selected for translation is sent to the chosen provider.
Google mode sends readable text segments and language codes to Google, without
an API key or a local AI model. Model API mode uses the configured AI endpoint.
Only one provider is selected at a time; there is no automatic AI/Google fallback.
Translations persist locally under config/simple_translate_web between sessions.
Runtime cache and configuration files may contain private text or credentials:
do not include them when redistributing this project.
The optional server cache sharing feature can share cached entries with a server
running the compatible server component when enabled.

## Availability

Google translation uses unofficial web endpoints with request pacing, caching,
batching and temporary cooldowns on rate limits. Availability and latency are not
guaranteed. Failed translations leave the original visible.

## Build from source

This archive contains the Fabric 26.2 source tree from commit 0b89d2e.
With JDK 25, change directory to fabric/SimpleTranslate-Fabric-26.2 and run:

    gradlew.bat test build

On Linux/macOS:

    sh gradlew test build

Build outputs are under that project's build/libs directory.
Live Google tests are opt-in: add -PliveGoogleTest.
