package com.yourname.simpletranslate.transport;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class GoogleWebFailoverTest {
    @Test
    void rateLimitedHostFallsBackAndIsSkippedUntilCooldownExpires() throws Exception {
        var blockedCalls = new AtomicInteger();
        var goodCalls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/blocked", exchange -> {
            blockedCalls.incrementAndGet();
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        server.createContext("/good", exchange -> {
            goodCalls.incrementAndGet();
            var body = "[[[\"Свежий перевод\",\"source\"]]]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var service = new GoogleWebTranslationService(List.of(base + "/blocked", base + "/good"));
            assertEquals(List.of("Свежий перевод"), service.translateTexts(
                    List.of("First phrase"), "en", "ru", "hud.dialogue", List.of()).get(5, TimeUnit.SECONDS));
            assertEquals(List.of("Свежий перевод"), service.translateTexts(
                    List.of("Second phrase"), "en", "ru", "hud.dialogue", List.of()).get(5, TimeUnit.SECONDS));
            assertEquals(1, blockedCalls.get(), "Do not keep sending to a rate-limited endpoint");
            assertEquals(2, goodCalls.get(), "Both uncached phrases must actually reach the reserve endpoint");
            assertEquals("", service.lastError());
        } finally { server.stop(0); }
    }

    @Test
    void reportsActualHttpFailureAndDoesNotCacheAnErrorAsTranslation() throws Exception {
        var calls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/failed", exchange -> {
            int status = calls.incrementAndGet() == 1 ? 500 : 200;
            var body = "[[[\"Повтор удался\",\"source\"]]]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var service = new GoogleWebTranslationService(List.of(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/failed"));
            assertThrows(Exception.class, () -> service.translateTexts(List.of("Retry this"), "en", "ru",
                    "hud.dialogue", List.of()).get(5, TimeUnit.SECONDS));
            assertEquals("Google HTTP 500", service.lastError());
            assertEquals(List.of("Повтор удался"), service.translateTexts(List.of("Retry this"), "en", "ru",
                    "hud.dialogue", List.of()).get(5, TimeUnit.SECONDS));
            assertEquals(2, calls.get());
        } finally { server.stop(0); }
    }
}
