package dev.learninggame.job;

import dev.learninggame.config.GameLogConfig;
import dev.learninggame.domain.AssetModerationRouter;
import dev.learninggame.domain.ModerationDecision;
import dev.learninggame.domain.PlayerAssetSubmission;
import dev.learninggame.logging.InfraiLogClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GameAssetLogJob {
    public static void main(String[] args) {
        GameLogConfig config = GameLogConfig.load();
        InfraiLogClient logs = new InfraiLogClient(config);
        PlayerAssetSubmission submission = new PlayerAssetSubmission(
                "asset-map-2048", "player-104", "lesson-map", "fractions-cup", true);
        ModerationDecision decision = new AssetModerationRouter().route(submission);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("game_title", config.gameTitle());
        context.put("environment", config.environment());
        context.put("event_type", "player_asset_review");
        context.put("asset_id", submission.assetId());
        context.put("player_id", submission.playerId());
        context.put("asset_type", submission.assetType());
        context.put("live_event_id", submission.liveEventId());
        context.put("moderation_queue", decision.queue());
        context.put("decision_reason", decision.reason());
        context.put("occurred_at", Instant.now().toString());

        Map<String, Object> log = new LinkedHashMap<>();
        log.put("level", "info");
        log.put("message", "Player asset routed for moderation");
        log.put("context", context);

        String ingestEnvelope = logs.ingest(log, "asset-review-" + submission.assetId());
        String searchEnvelope = logs.search(config.searchQuery());
        System.out.println("Routed " + submission.assetId() + " to " + decision.queue());
        System.out.println("Ingest envelope: " + ingestEnvelope);
        System.out.println("Search envelope: " + searchEnvelope);
    }
}

