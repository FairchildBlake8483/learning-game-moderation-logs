package dev.learninggame.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public record GameLogConfig(String baseUrl, String apiKey, String gameTitle,
                            String environment, String searchQuery) {
    public static GameLogConfig load() {
        return load(System.getenv());
    }

    static GameLogConfig load(Map<String, String> environment) {
        Properties properties = new Properties();
        try (InputStream input = GameLogConfig.class.getResourceAsStream("/application.properties")) {
            if (input == null) {
                throw new IllegalStateException("application.properties is required");
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load application.properties", exception);
        }

        String apiKey = environment.get("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running the job");
        }
        return new GameLogConfig(
                environment.getOrDefault("INFRAI_BASE_URL", properties.getProperty("infrai.base-url")),
                apiKey,
                environment.getOrDefault("GAME_TITLE", properties.getProperty("game.title")),
                environment.getOrDefault("GAME_ENVIRONMENT", properties.getProperty("game.environment")),
                environment.getOrDefault("LOG_SEARCH_QUERY", properties.getProperty("logs.search-query")));
    }
}

