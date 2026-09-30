#!/usr/bin/env bash
# Sets up the plant-identification evaluation: a venv, the LiteRT runtime, and
# Google's AIY plants_V1 classifier. Separate from the app; nothing here ships.
set -euo pipefail
cd "$(dirname "$0")"

echo "[1/4] venv"
python3 -m venv .venv

echo "[2/4] runtime (ai-edge-litert is the successor to tflite-runtime, which has no 3.12 wheels)"
.venv/bin/python -m pip install -q --upgrade pip
.venv/bin/python -m pip install -q ai-edge-litert numpy pillow

echo "[3/4] model"
mkdir -p model
curl -sL -o model/bundle.tar.gz \
  "https://tfhub.dev/google/aiy/vision/classifier/plants_V1/1?tf-hub-format=compressed"
tar -xzf model/bundle.tar.gz -C model
echo "  contents:"
find model -type f | sed 's/^/    /'

echo "[4/4] done"
