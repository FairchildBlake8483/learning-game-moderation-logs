package dev.learninggame.domain;

public final class AssetModerationRouter {
    public ModerationDecision route(PlayerAssetSubmission submission) {
        boolean liveAndPublic = submission.publicAudience()
                && submission.liveEventId() != null
                && !submission.liveEventId().isBlank();
        if (liveAndPublic) {
            return new ModerationDecision("live-event-priority", "public asset in an active lesson event");
        }
        return new ModerationDecision("standard-review", "asset is outside a public live event");
    }
}

