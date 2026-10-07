#!/usr/bin/env bash
# Watch a 7-shot hero render. Rate and ETA are measured, never guessed.
#   tools/blender/render-progress.sh [outdir]   (--watch to loop)
OUT=${1:-/tmp/r7}
loop=0; [ "${2:-}" = "--watch" ] && loop=1
[ "${1:-}" = "--watch" ] && { OUT=/tmp/r7; loop=1; }
while :; do
  tot=$(cat "$OUT/.total" 2>/dev/null || echo 7)
  done_n=$(wc -l < "$OUT/.done" 2>/dev/null || echo 0)
  st=$(cat "$OUT/.start" 2>/dev/null || date +%s)
  el=$(( $(date +%s) - st ))
  if [ "$done_n" -gt 0 ]; then
    per=$(( el / done_n )); left=$(( (tot - done_n) * per ))
    eta=$(printf '%dm%02ds' $((left/60)) $((left%60)))
    rate="${per}s/shot"
  else
    eta='--'; rate='measuring'
  fi
  printf '\r  %s  %d/%d shots  elapsed %dm%02ds  %s  ETA %s    ' \
    "$([ -f "$OUT/.finished" ] && echo DONE || echo running)" \
    "$done_n" "$tot" $((el/60)) $((el%60)) "$rate" "$eta"
  [ -f "$OUT/.finished" ] && { echo; break; }
  [ "$loop" = 1 ] || { echo; break; }
  sleep 20
done
