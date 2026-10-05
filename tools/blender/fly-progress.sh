#!/usr/bin/env bash
# Progress of a flythrough render: frames done out of total, elapsed, and an ETA
# derived from the measured rate. No ETA until the first frame lands, because a
# guessed one here has been wrong by an order of magnitude before.
OUT="${1:-/home/dheirav/Code/ThirstTrap/tools/blender/out-fly}"
S=$(date +%s)
while :; do
  n=$(ls "$OUT"/f*.png 2>/dev/null | wc -l)
  t=$(python3 -c "import json;print(json.load(open('$OUT/status.json'))['total'])" 2>/dev/null || echo "?")
  e=$(( $(date +%s) - S ))
  if [ "$n" -gt 0 ] && [ "$t" != "?" ]; then
    r=$(echo "scale=2;$e/$n" | bc); eta=$(echo "scale=0;($t-$n)*$r/1" | bc)
    printf "\r  %3d/%s  elapsed %02d:%02d  %.1fs/frame  ETA %02d:%02d   " \
      "$n" "$t" $((e/60)) $((e%60)) "$r" $((eta/60)) $((eta%60))
    [ "$n" -ge "$t" ] && { echo; break; }
  else
    printf "\r  %3d frames  elapsed %02d:%02d  (no rate yet, no ETA)   " "$n" $((e/60)) $((e%60))
  fi
  sleep 4
done
