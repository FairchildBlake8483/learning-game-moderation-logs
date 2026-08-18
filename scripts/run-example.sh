#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BUILD="$ROOT/build/classes"
rm -rf "$ROOT/build"
mkdir -p "$BUILD"
find "$ROOT/src/main/java" "$ROOT/src/test/java" -name '*.java' -print > "$ROOT/build/sources.txt"
javac -d "$BUILD" @"$ROOT/build/sources.txt"
cp "$ROOT/src/main/resources/application.properties" "$BUILD/application.properties"
java -cp "$BUILD" dev.learninggame.domain.AssetModerationRouterTest
java -cp "$BUILD" dev.learninggame.job.GameAssetLogJob

