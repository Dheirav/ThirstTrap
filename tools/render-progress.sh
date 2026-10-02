#!/usr/bin/env bash
# Progress for a Blender frame-sequence render.
#
# Counts finished PNGs against the target and derives the rate from frames
# actually completed since the run started. No guessed ETA: a render rate
# depends on the scene, and a figure invented up front has been wrong here by
# an order of magnitude in both directions.
#
#   tools/render-progress.sh <out_dir> <total_frames> [--watch]
set -u
DIR="${1:?usage: render-progress.sh <out_dir> <total> [--watch]}"
TOTAL="${2:?need a total frame count}"
WATCH="${3:-}"
STAMP="$DIR/.started"
# Time from the OLDEST frame on disk, not from when this script first ran.
# A stamp file written on first call reports a rate of nothing when the
# directory was recreated at launch, which is exactly when you want the rate.
first_frame_time() {
  find "$DIR" -maxdepth 1 -name 'f_*.png' -printf '%T@\n' 2>/dev/null \
    | sort -n | head -1 | cut -d. -f1
}

show() {
  local start now done_ el rate eta etas pct bar filled
  now=$(date +%s)
  start=$(first_frame_time); [ -z "$start" ] && start=$now
  done_=$(find "$DIR" -maxdepth 1 -name 'f_*.png' 2>/dev/null | wc -l)
  el=$(( now - start )); [ "$el" -lt 1 ] && el=1
  pct=$(( done_ * 100 / TOTAL ))
  filled=$(( pct / 4 )); bar=$(printf '%*s' "$filled" '' | tr ' ' '#')
  if [ "$done_" -gt 0 ]; then
    rate=$(awk -v d="$done_" -v e="$el" 'BEGIN{printf "%.2f", e/d}')
    eta=$(awk -v r="$rate" -v n="$(( TOTAL - done_ ))" 'BEGIN{printf "%d", r*n}')
    etas="$(( eta / 60 ))m$(( eta % 60 ))s"
  else
    rate="--"; etas="no rate yet"
  fi
  printf '\r[%-25s] %3d%%  %3d/%-3d frames  elapsed %dm%02ds  %ss/frame  ETA %s   ' \
    "$bar" "$pct" "$done_" "$TOTAL" "$(( el / 60 ))" "$(( el % 60 ))" "$rate" "$etas"
  [ "$done_" -ge "$TOTAL" ]
}

if [ "$WATCH" = "--watch" ]; then
  while ! show; do sleep 5; done; echo; echo "render complete: $DIR"
else show; echo; fi
