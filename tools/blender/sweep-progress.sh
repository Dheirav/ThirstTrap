#!/usr/bin/env bash
# Watch a sweep. ETA is derived from the rate actually measured so far, and is
# withheld until at least one render has finished, because a guessed ETA here
# has been wrong by an order of magnitude in both directions.
set -u
OUT=${1:-/tmp/sweep}; W=${2:-}
show() {
  [ -f "$OUT/.total" ] || { echo "no sweep at $OUT"; return 1; }
  local tot done start now el rate eta
  tot=$(cat "$OUT/.total"); done=$( [ -f "$OUT/.done" ] && wc -l < "$OUT/.done" || echo 0)
  start=$(cat "$OUT/.start"); now=$(date +%s); el=$((now - start))
  printf '\r%s%2d/%-2d renders  elapsed %dm%02ds  ' \
    "$( [ -f "$OUT/.finished" ] && echo 'DONE ' || echo '')" "$done" "$tot" $((el/60)) $((el%60))
  if [ "$done" -gt 0 ]; then
    rate=$((el / done)); eta=$(( rate * (tot - done) ))
    printf '%ds/render  ETA %dm%02ds   ' "$rate" $((eta/60)) $((eta%60))
  else
    printf 'no rate yet, so no ETA   '
  fi
}
if [ "$W" = "--watch" ]; then
  while :; do show || exit 1; [ -f "$OUT/.finished" ] && { echo; exit 0; }; sleep 5; done
else show; echo; fi
