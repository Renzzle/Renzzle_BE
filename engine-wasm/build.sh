#!/usr/bin/env bash
# Rebuilds the share page's AI engine from Renzzle_AI at AI_COMMIT. Needs git and Docker.
# Bump AI_COMMIT when the engine changes, run this, and commit the two files it writes.
set -euo pipefail

AI_REPO=https://github.com/Renzzle/Renzzle_AI.git
AI_COMMIT=193be8cd20156ca9bffb8e6975da3011ea4d75ce
EMSDK_IMAGE=emscripten/emsdk:6.0.13

ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT="$ROOT/src/main/resources/static/assets/engine"
WORK=$(mktemp -d)
trap 'cd "$ROOT" && rm -rf "$WORK"' EXIT

# Relative paths from here, since Git for Windows reads /tmp/... as C:\tmp\...
cd "$WORK"
git clone --quiet "$AI_REPO" ai
git -C ai checkout --quiet "$AI_COMMIT"
cp "$ROOT/engine-wasm/renzzle_web.cpp" .
echo '// © 2026 Studio Isoo. All rights reserved.' > copyright.js

# Docker Desktop on Windows needs a Windows path for the mount
MOUNT="$WORK"
if command -v cygpath > /dev/null; then
  MOUNT=$(cygpath -w "$WORK")
fi

# The default 64KB stack overflows in deep searches, so the stack is 8MB; searches peak near 97MB
MSYS_NO_PATHCONV=1 docker run --rm -v "$MOUNT:/src" -w /src "$EMSDK_IMAGE" bash -c '
  set -e
  em++ -O3 -std=c++17 -Iai renzzle_web.cpp -o renzzle-engine.mjs \
    -sMODULARIZE -sEXPORT_ES6 -sENVIRONMENT=worker \
    -sSTACK_SIZE=8MB -sINITIAL_MEMORY=32MB -sALLOW_MEMORY_GROWTH -sMAXIMUM_MEMORY=128MB \
    -sEXPORTED_FUNCTIONS=_renzzle_next_move,_renzzle_check_win \
    -sEXPORTED_RUNTIME_METHODS=ccall \
    --extern-pre-js copyright.js
'

mkdir -p "$OUT"
cp renzzle-engine.mjs renzzle-engine.wasm "$OUT/"
echo "Built from Renzzle_AI $AI_COMMIT into $OUT"
