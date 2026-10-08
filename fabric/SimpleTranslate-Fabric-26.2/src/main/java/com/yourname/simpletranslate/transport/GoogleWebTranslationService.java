package com.yourname.simpletranslate.transport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.yourname.simpletranslate.SimpleTranslateMod;
import com.yourname.simpletranslate.api.TranslationRequest;
import com.yourname.simpletranslate.api.TranslationResult;
import com.yourname.simpletranslate.api.TranslationService;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

/** Keyless Google web translation. Only semantic text slots leave the client. */
public final class GoogleWebTranslationService implements TranslationService {
    static final String ENDPOINT = "https://translate.google.com/_/TranslateWebserverUi/data/batchexecute";
    private static final List<String> ENDPOINTS = List.of(ENDPOINT,
            "https://translate.google.com/translate_a/single",
            "https://translate.googleapis.com/translate_a/single");
    static final String BATCH_MARKER = "[91827364509182736450]";
    static final String SEPARATOR = "\n" + BATCH_MARKER + "\n";
    private static final int MAX_ENCODED_CHARS = 7000;
    private static final int MAX_TEXT_CHARS = 4500;
    private static final int CACHE_LIMIT = 4096;
    // Google's keyless endpoint returns 429 to Java's HTTP/2 connection on some
    // networks, while the same text over HTTP/1.1 succeeds. Pin the wire protocol.
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).version(HttpClient.Version.HTTP_1_1).build();
    private final List<String> endpoints;
    private final Map<String, String> memory = new LinkedHashMap<>(256, 0.75f, true);
    private final Map<String, Long> cooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private final Object pacingLock = new Object();
    private long nextRequestAt;
    private volatile String lastError = "";

    public GoogleWebTranslationService() { this(ENDPOINTS); }

    GoogleWebTranslationService(List<String> endpoints) {
        this.endpoints = List.copyOf(endpoints);
    }

    @Override
    public CompletableFuture<TranslationResult> translate(TranslationRequest request) {
        try {
            String sourceLanguage = googleLanguage(request.sourceLanguage(), true);
            String targetLanguage = googleLanguage(request.targetLanguage(), false);
            JsonArray slots = JsonParser.parseString(String.join("\n", request.lines())).getAsJsonArray();
            List<String> texts = new ArrayList<>();
            for (JsonElement slot : slots) {
                // ComponentVisualProjection emits literal strings without styles,
                // identifiers, PUA, click/hover events, or JSON metadata.
                if (!slot.isJsonPrimitive() || !slot.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Expected a semantic text slot");
                }
                texts.add(slot.getAsString());
            }
            return translateTexts(texts, sourceLanguage, targetLanguage, request.surface(), request.terms())
                    .<TranslationResult>thenApply(translations -> {
                        JsonArray output = new JsonArray();
                        translations.forEach(output::add);
                        return new TranslationResult.Success(output.toString());
                    }).exceptionally(error -> new TranslationResult.Failed(errorReason(error)));
        } catch (RuntimeException error) {
            return CompletableFuture.completedFuture(new TranslationResult.Failed(errorReason(error)));
        }
    }

    CompletableFuture<List<String>> translateTexts(List<String> texts, String sourceLanguage,
            String targetLanguage, String surface, List<TranslationRequest.Term> terms) {
        String pair = sourceLanguage + ":" + targetLanguage + ":";
        Map<String, CompletableFuture<String>> results = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        for (String text : texts) {
            if (results.containsKey(text)) continue;
            String known;
            synchronized (memory) { known = memory.get(pair + text); }
            for (TranslationRequest.Term term : terms) {
                if (term.source().equals(text) && !term.target().isBlank()) known = term.target();
            }
            if (text.isBlank() || sourceLanguage.equals(targetLanguage)) known = text;
            if (known != null) results.put(text, CompletableFuture.completedFuture(known));
            else {
                results.put(text, new CompletableFuture<>());
                missing.add(text);
            }
        }
        for (List<String> batch : batches(missing)) {
            translateBatch(batch, sourceLanguage, targetLanguage, surface)
                    .whenComplete((translated, error) -> {
                        for (int index = 0; index < batch.size(); index++) {
                            String text = batch.get(index);
                            if (error != null) results.get(text).completeExceptionally(error);
                            else {
                                String value = translated.get(index);
                                synchronized (memory) {
                                    memory.put(pair + text, value);
                                    while (memory.size() > CACHE_LIMIT) memory.remove(memory.keySet().iterator().next());
                                }
                                results.get(text).complete(value);
                            }
                        }
                    });
        }
        return CompletableFuture.allOf(results.values().toArray(CompletableFuture[]::new))
                .thenApply(ignored -> texts.stream().map(text -> results.get(text).join()).toList());
    }

    private CompletableFuture<List<String>> translateBatch(List<String> texts, String source,
            String target, String surface) {
        if (texts.size() == 1) {
            return translateLongText(texts.getFirst(), source, target, surface).thenApply(List::of);
        }
        String joined = String.join(SEPARATOR, texts);
        return request(joined, source, target, surface).thenCompose(translated -> {
            String[] parts = translated.split("\\s*" + Pattern.quote(BATCH_MARKER) + "\\s*", -1);
            if (parts.length == texts.size() && java.util.Arrays.stream(parts).noneMatch(String::isBlank)) {
                return CompletableFuture.completedFuture(List.of(parts));
            }
            // An undocumented endpoint may alter the delimiter. Never put one
            // item's translation in another slot; retry those texts separately.
            List<CompletableFuture<String>> singles = texts.stream()
                    .map(text -> translateLongText(text, source, target, surface)).toList();
            return CompletableFuture.allOf(singles.toArray(CompletableFuture[]::new))
                    .thenApply(ignored -> singles.stream().map(CompletableFuture::join).toList());
        });
    }

    private CompletableFuture<String> translateLongText(String text, String source, String target, String surface) {
        List<String> chunks = splitLongText(text);
        List<CompletableFuture<String>> futures = chunks.stream()
                .map(chunk -> request(chunk, source, target, surface)).toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> String.join(" ", futures.stream().map(CompletableFuture::join).toList()));
    }

    private CompletableFuture<String> request(String text, String source, String target, String surface) {
        String key = "google-web:" + source + ":" + target + ":" + text;
        TranslationRequestQueue.Priority priority = surface.contains("dialogue")
                ? TranslationRequestQueue.Priority.TITLE_URGENT
                : surface.startsWith("chat") ? TranslationRequestQueue.Priority.CHAT
                : surface.contains("tooltip") ? TranslationRequestQueue.Priority.INTERACTIVE
                : TranslationRequestQueue.Priority.NORMAL;
        return TranslationRequestQueue.submit(key, surface, priority, 1, () -> {
            return sendToEndpoint(0, text, source, target, surface);
        }).thenApply(value -> {
            if (value == null || value.isBlank()) throw new IllegalStateException("Google translation unavailable");
            return value;
        });
    }

    private CompletableFuture<String> sendToEndpoint(int index, String text, String source,
            String target, String surface) {
        if (index >= endpoints.size()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    lastError.isBlank() ? "Google temporarily unavailable" : lastError));
        }
        String endpoint = endpoints.get(index);
        Long cooldown = cooldowns.get(endpoint);
        if (cooldown != null && System.nanoTime() - cooldown < 0L) {
            return sendToEndpoint(index + 1, text, source, target, surface);
        }
        long delay;
        synchronized (pacingLock) {
            long now = System.nanoTime();
            long scheduled = Math.max(now, nextRequestAt);
            delay = Math.max(0L, scheduled - now);
            nextRequestAt = scheduled + Duration.ofMillis(350).toNanos();
        }
        return CompletableFuture.supplyAsync(() -> {
            if (isWebRpc(endpoint)) {
                return HttpRequest.newBuilder(URI.create(endpoint + "?rpcids=MkEWBc&source-path=%2F&rt=c"))
                        .timeout(Duration.ofSeconds(8)).header("User-Agent", "SimpleTranslate-Web/2.2.1")
                        .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(webRpcBody(text, source, target), StandardCharsets.UTF_8))
                        .build();
            }
            return HttpRequest.newBuilder(URI.create(endpoint + "?client=gtx&sl=" + source + "&tl=" + target
                            + "&dt=t&q=" + encode(text))).timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "SimpleTranslate-Web/2.2.1").GET().build();
        }, CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.NANOSECONDS))
                .thenCompose(httpRequest -> {
                    long started = System.nanoTime();
                    return client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                            .thenApply(response -> {
                                if (response.statusCode() != 200) {
                                    lastError = "Google HTTP " + response.statusCode();
                                    if (response.statusCode() == 429 || response.statusCode() == 403
                                            || response.statusCode() == 503) {
                                        cooldowns.put(endpoint, System.nanoTime() + Duration.ofSeconds(60).toNanos());
                                    }
                                    throw new IllegalStateException(lastError);
                                }
                                String value = isWebRpc(endpoint)
                                        ? parseWebRpcResponse(response.body()) : parseResponse(response.body());
                                lastError = "";
                                SimpleTranslateMod.getLogger().info(
                                        "Google WEB translated surface={} chars={} durationMs={} host={} transport={}",
                                        surface, text.length(), (System.nanoTime() - started) / 1_000_000L,
                                        URI.create(endpoint).getHost(), isWebRpc(endpoint) ? "web-rpc" : "gtx");
                                return value;
                            });
                }).exceptionallyCompose(error -> {
                    lastError = errorReason(error);
                    SimpleTranslateMod.getLogger().warn(
                            "Google WEB request failed surface={} host={} protocol=HTTP/1.1 reason={}",
                            surface, URI.create(endpoint).getHost(), lastError);
                    return sendToEndpoint(index + 1, text, source, target, surface);
                });
    }

    public CompletableFuture<String> probe() {
        return request("Welcome to Wynncraft!", "en", "ru", "manager.probe");
    }

    public String lastError() { return lastError; }

    private static boolean isWebRpc(String endpoint) { return endpoint.endsWith("/batchexecute"); }

    static String webRpcBody(String text, String source, String target) {
        JsonArray input = new JsonArray();
        input.add(text); input.add(source); input.add(target); input.add(true);
        JsonArray flags = new JsonArray(); flags.add(com.google.gson.JsonNull.INSTANCE);
        JsonArray parameters = new JsonArray(); parameters.add(input); parameters.add(flags);
        JsonArray call = new JsonArray();
        call.add("MkEWBc"); call.add(parameters.toString());
        call.add(com.google.gson.JsonNull.INSTANCE); call.add("generic");
        JsonArray calls = new JsonArray(); calls.add(call);
        JsonArray envelope = new JsonArray(); envelope.add(calls);
        return "f.req=" + encode(envelope.toString());
    }

    static String parseWebRpcResponse(String body) {
        // batchexecute frames have an XSSI prefix, length lines, and JSON rows.
        // Read only the translation RPC, never suggestions or pronunciation.
        for (String line : body.split("\\R")) {
            if (!line.stripLeading().startsWith("[[")) continue;
            for (JsonElement row : JsonParser.parseString(line).getAsJsonArray()) {
                if (!row.isJsonArray()) continue;
                JsonArray values = row.getAsJsonArray();
                if (values.size() < 3 || !values.get(0).isJsonPrimitive()
                        || !"wrb.fr".equals(values.get(0).getAsString())
                        || !"MkEWBc".equals(values.get(1).getAsString()) || values.get(2).isJsonNull()) continue;
                JsonArray payload = JsonParser.parseString(values.get(2).getAsString()).getAsJsonArray();
                JsonArray sentences = payload.get(1).getAsJsonArray().get(0).getAsJsonArray()
                        .get(0).getAsJsonArray().get(5).getAsJsonArray();
                StringBuilder translated = new StringBuilder();
                for (JsonElement sentence : sentences) {
                    JsonElement value = sentence.getAsJsonArray().get(0);
                    if (!value.isJsonNull()) {
                        String part = value.getAsString();
                        if (!translated.isEmpty() && !part.isEmpty()
                                && !Character.isWhitespace(translated.charAt(translated.length() - 1))
                                && !Character.isWhitespace(part.charAt(0))) translated.append(' ');
                        translated.append(part);
                    }
                }
                if (!translated.toString().isBlank()) return translated.toString();
            }
        }
        throw new IllegalArgumentException("Google web translator returned no translation");
    }

    static String parseResponse(String body) {
        JsonArray response = JsonParser.parseString(body).getAsJsonArray();
        StringBuilder result = new StringBuilder();
        for (JsonElement sentence : response.get(0).getAsJsonArray()) {
            JsonElement value = sentence.getAsJsonArray().get(0);
            if (!value.isJsonNull()) result.append(value.getAsString());
        }
        if (result.toString().isBlank()) throw new IllegalArgumentException("Empty Google response");
        return result.toString();
    }

    static String googleLanguage(String code, boolean allowAuto) {
        String value = code == null ? "" : code.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        if (value.isEmpty()) value = allowAuto ? "auto" : "ru";
        if (value.equals("auto")) {
            if (allowAuto) return value;
            throw new IllegalArgumentException("Target language cannot be auto");
        }
        value = switch (value) {
            case "zh", "zh-cn", "zh-hans" -> "zh-CN";
            case "zh-tw", "zh-hant", "zh-hk" -> "zh-TW";
            case "pt-br", "pt-pt" -> value;
            default -> value.split("-", 2)[0];
        };
        if (!Pattern.matches("[a-zA-Z]{2,3}(?:-[a-zA-Z]{2,4})?", value)) {
            throw new IllegalArgumentException("Invalid language code");
        }
        return value;
    }

    static List<List<String>> batches(List<String> texts) {
        List<List<String>> batches = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String text : texts) {
            if (text.contains(BATCH_MARKER) || text.length() > MAX_TEXT_CHARS
                    || encode(text).length() > MAX_ENCODED_CHARS) {
                if (!current.isEmpty()) { batches.add(List.copyOf(current)); current.clear(); }
                batches.add(List.of(text));
                continue;
            }
            List<String> candidate = new ArrayList<>(current);
            candidate.add(text);
            if (!current.isEmpty() && (candidate.size() > 16
                    || String.join(SEPARATOR, candidate).length() > MAX_TEXT_CHARS
                    || encode(String.join(SEPARATOR, candidate)).length() > MAX_ENCODED_CHARS)) {
                batches.add(List.copyOf(current)); current.clear();
            }
            current.add(text);
        }
        if (!current.isEmpty()) batches.add(List.copyOf(current));
        return batches;
    }

    static List<String> splitLongText(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = start;
            int encoded = 0;
            int lastSpace = -1;
            while (end < text.length()) {
                int next = end + Character.charCount(text.codePointAt(end));
                int size = encode(text.substring(end, next)).length();
                if (encoded + size > MAX_ENCODED_CHARS || next - start > MAX_TEXT_CHARS) break;
                encoded += size;
                if (Character.isWhitespace(text.codePointAt(end))) lastSpace = next;
                end = next;
            }
            if (end < text.length() && lastSpace > start) end = lastSpace;
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }

    private static String encode(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8); }

    private static String errorReason(Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) error = error.getCause();
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
