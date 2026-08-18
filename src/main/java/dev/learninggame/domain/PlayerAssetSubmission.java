package dev.learninggame.domain;

public record PlayerAssetSubmission(String assetId, String playerId, String assetType,
                                    String liveEventId, boolean publicAudience) {
    public PlayerAssetSubmission {
        if (assetId.isBlank() || playerId.isBlank() || assetType.isBlank()) {
            throw new IllegalArgumentException("Asset, player, and type are required");
        }
    }
}

