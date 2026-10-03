#!/usr/bin/env bash
# Progress for a full room render. Fourteen frames, rate measured from the gaps
# between frames already written rather than assumed.
set -u
OUT="${1:-/home/dheirav/Code/ThirstTrap/tools/blender/out}"
TOTAL=14
hms(){ printf '%dm%02ds' $(( $1/60 )) $(( $1%60 )); }
report(){
  local n t0 t1 now rate rem eta
  n=$(ls "$OUT"/*.png 2>/dev/null | wc -l)
  now=$(date +%s)
  if [ "$n" -ge 1 ]; then
    t0=$(ls -t --time=birth "$OUT"/*.png 2>/dev/null | tail -1 | xargs stat -c %Y 2>/dev/null)
    t1=$(ls -t "$OUT"/*.png 2>/dev/null | head -1 | xargs stat -c %Y 2>/dev/null)
  fi
  printf '\r\033[K%2d/%d frames' "$n" "$TOTAL"
  if [ "${n:-0}" -ge 2 ]; then
    printf '  elapsed %s' "$(hms $(( t1 - t0 )) )"
    rate=$(( (t1 - t0) / (n - 1) )); rem=$(( TOTAL - n ))
    if [ "$rate" -gt 0 ] && [ "$rem" -gt 0 ]; then
      eta=$(( rem * rate ))
      printf '  rate %s/frame  ETA %s (IST %s)' "$(hms $rate)" "$(hms $eta)" "$(date -d "@$((now+eta))" '+%H:%M')"
    fi
  else
    printf '  no rate yet, an ETA needs two finished frames'
  fi
  [ "${n:-0}" -ge "$TOTAL" ] && { printf '  DONE\n'; return 1; }
  return 0
}
if [ "${2:-}" = "--watch" ] || [ "${1:-}" = "--watch" ]; then while report; do sleep 8; done; else report; echo; fi
