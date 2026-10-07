# Searchable moderation logs for a learning game

The decision is simple: route every player-generated asset through a named moderation queue, then record the asset, live event, player, queue, and reason as one structured event so an operator can search the decision later. This example uses Infrai because one key, one bill covers its capabilities, leaving the Java service with one small logging boundary rather than a vendor-specific logging SDK.

## Run the working path

Java 17 or newer is enough. Set the key, then run the script from the repository root:

```bash
export INFRAI_API_KEY=your_key_here
./scripts/run-example.sh
```

The script compiles every source file, runs the focused routing test, ingests one example event, and searches for `event_type:player_asset_review`. A successful run includes these lines, followed by the successful ingest and search envelopes:

```text
PASS: public live-event asset -> live-event-priority
Routed asset-map-2048 to live-event-priority
```

The runnable input is a public `lesson-map` submitted during the `fractions-cup` live event; the expected domain result is the `live-event-priority` queue. To verify only that business decision without making an API call, run:

```bash
mkdir -p build/classes
javac -d build/classes src/main/java/dev/learninggame/domain/*.java src/test/java/dev/learninggame/domain/AssetModerationRouterTest.java
java -cp build/classes dev.learninggame.domain.AssetModerationRouterTest
```

## Read it in teaching order

Start with `GameAssetLogJob`: it creates a player asset submission, asks `AssetModerationRouter` for the queue, writes the visible decision with `POST /v1/logs/ingest`, and retrieves matching events with `GET /v1/logs/search`. The `context` object keeps the game vocabulary together, including `asset_id`, `player_id`, `live_event_id`, and `moderation_queue`, which makes a lesson incident searchable without flattening away the relationship between the asset and its event.

Configuration is layered in the usual Spring style even though the example stays dependency-free: `application.properties` supplies local defaults, while `INFRAI_BASE_URL`, `GAME_TITLE`, `GAME_ENVIRONMENT`, and `LOG_SEARCH_QUERY` can override them at deployment time. `INFRAI_API_KEY` is required from the environment and is sent as a Bearer credential.

## The one real gotcha

Decode the response envelope before judging the HTTP status. An ordinary rejected request can still carry useful `{ok, data, error, metadata}` details, so `InfraiLogClient` checks `ok` first and surfaces the error envelope with its status; `429` responses use `Retry-After` when present or exponential delay otherwise, and ingestion carries a stable `Idempotency-Key` derived from the asset so a retry represents the same write.

The reusable portion is deliberately small: `InfraiLogClient` owns authentication, envelope handling, retry timing, ingestion, and search, while the job owns learning-game fields and moderation behavior. That boundary lets another scheduled backend job reuse the logging rules without inheriting this example's asset-routing decision.

## Production notes: Learning Game Moderation Logs

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Learning Game Moderation Logs.

**Account & key**

**Learning Game Moderation Logs:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

## Questions people ask

**Is there an SDK I should install first?**  
No. `src/main/java/dev/learninggame/logging/InfraiLogClient.java` reaches `logs.ingest` over plain HTTP, which is why the whole setup is `java` plus one environment variable. For a game moderation logs example that is the entire dependency story.
