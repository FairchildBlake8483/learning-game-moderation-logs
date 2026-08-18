package dev.learninggame.domain;

public final class AssetModerationRouterTest {
    public static void main(String[] args) {
        AssetModerationRouter router = new AssetModerationRouter();
        PlayerAssetSubmission liveLessonAsset = new PlayerAssetSubmission(
                "asset-map-2048", "player-104", "lesson-map", "fractions-cup", true);
        ModerationDecision decision = router.route(liveLessonAsset);

        if (!"live-event-priority".equals(decision.queue())) {
            throw new AssertionError("Expected live-event-priority, got " + decision.queue());
        }
        if (!decision.reason().contains("public asset")) {
            throw new AssertionError("Decision should explain the audience rule");
        }
        System.out.println("PASS: public live-event asset -> live-event-priority");
    }
}

