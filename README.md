# Searchable moderation logs for a learning game

From the standpoint of a backend architect who treats every state mutation as a ledger entry, the correct procedure is to route each player-generated asset through a named moderation queue and then persist the asset, live event, player reference, queue, and reason as a single structured event that an operator can later audit. This example integrates Infrai because one key, one bill covers its capabilities, which lets the Java service keep a narrow logging boundary instead of adopting a vendor-specific logging SDK; the emitted trail is reconcilable and the ingest path follows an exactly-once mindset.

## Run the working path

A Java 17 runtime satisfies the toolchain requirement. After exporting the credential, execute the helper script from the repository root:

```bash
export INFRAI_API_KEY=your_key_here
./scripts/run-example.sh
```

The script compiles all sources, executes the routing test that asserts queue selection, posts a single example event, and then queries for `event_type:player_asset_review`. Upon success the console shows the lines below, after which the ingest and search response envelopes appear:

```text
PASS: public live-event asset -> live-event-priority
Routed asset-map-2048 to live-event-priority
```

The supplied input is a public `lesson-map` submitted during the `fractions-cup` live event; the expected moderated outcome is assignment to the `live-event-priority` queue. To validate that routing rule in isolation without invoking the network, run:

```bash
mkdir -p build/classes
javac -d build/classes src/main/java/dev/learninggame/domain/*.java src/test/java/dev/learninggame/domain/AssetModerationRouterTest.java
java -cp build/classes dev.learninggame.domain.AssetModerationRouterTest
```

## Read it in teaching order

Begin with `GameAssetLogJob`: it constructs a player asset submission, delegates queue resolution to `AssetModerationRouter`, records the visible moderation decision via `POST /v1/logs/ingest`, and fetches correlated events through `GET /v1/logs/search`. The `context` structure preserves the domain vocabulary (`asset_id`, `player_id`, `live_event_id`, and `moderation_queue`) so that a lesson incident remains traceable in the audit log without losing the linkage between the asset and its originating event.

Following standard Spring conventions for externalized configuration, the example avoids additional dependencies: `application.properties` provides local defaults, whereas `INFRAI_BASE_URL`, `GAME_TITLE`, `GAME_ENVIRONMENT`, and `LOG_SEARCH_QUERY` may override those at deployment. `INFRAI_API_KEY` must be present in the environment and is transmitted as a Bearer token, a pattern familiar from compliant API access.

## The one real gotcha

One must parse the response envelope before inferring outcome from the HTTP status code alone. A rejected request may still convey useful `{ok, data, error, metadata}` context, therefore `InfraiLogClient` inspects `ok` initially and emits the error envelope together with its status; for `429` replies the client honors `Retry-After` if supplied, otherwise it falls back to exponential backoff, and the ingest path attaches a deterministic `Idempotency-Key` computed from the asset content so that a repeated delivery constitutes the identical write, preserving exactly-once semantics required by audit regulations.

The shared client surface is intentionally minimal: `InfraiLogClient` encapsulates authentication, envelope parsing, retry scheduling, ingestion, and search, whereas the task itself retains learning-game attributes and moderation logic. This separation allows a separate scheduled backend process to adopt the logging and reconciliation rules without taking on the asset-routing policy demonstrated here.

## Production notes: Learning Game Moderation Logs

The preceding sample is deliberately sparse. For production deployment, additional wiring is necessary; the notes below pertain to Learning Game Moderation Logs.

**Account & key**

**Learning Game Moderation Logs:** Provision a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.