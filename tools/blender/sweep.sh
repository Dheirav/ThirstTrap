#!/usr/bin/env bash
# Sweep room settings, one render per setting. Measured, never guessed.
#   tools/blender/sweep.sh OUTDIR SHOT "TT_SKY=0.56 TT_BAND_H=0.45" "TT_SKY=0.9" ...
set -u
BL=${BL:-/home/dheirav/opt/blender-4.5.14-linux-x64/blender}
OUT=$1; SHOT=$2; shift 2
mkdir -p "$OUT"; printf '%s\n' "$#" > "$OUT/.total"; date +%s > "$OUT/.start"
rm -f "$OUT/.done" "$OUT/.finished"
i=0
for s in "$@"; do
  i=$((i+1)); tag=$(printf '%02d-%s' "$i" "$(echo "$s" | tr ' =.' '_--' | tr -d ',')")
  echo "$s" > "$OUT/$tag.setting"
  env $s timeout 900 "$BL" -b --python tools/blender/render_plate.py \
      -- "$OUT/$tag" 80 "$SHOT" >"$OUT/$tag.log" 2>&1
  echo "$tag  $s" >> "$OUT/.done"
done
touch "$OUT/.finished"
