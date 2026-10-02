#!/usr/bin/env bash
# Progress for a hero plate generation run.
#
# gen.mjs prints one line per scene when it finishes, which tells you nothing
# while a scene is in flight, and a scene takes minutes. This reads the output
# directory instead. The rate is measured from the gaps between plates that have
# already landed, never assumed: with fewer than two done there is no rate yet
# and it says so rather than inventing a number.
set -u
OUT="$(cd "$(dirname "$0")" && pwd)/out"
MARK="$OUT/.run-started"
TARGETS=(depth roots phone-closeup ledger-p shelf-evening-p finger-test-p \
         depth-p roots-p scale-table-p phone-closeup-p)
[ "${1:-}" = "--targets" ] && { shift; TARGETS=("$@"); }

hms() { local s=$1; printf '%dm%02ds' $((s/60)) $((s%60)); }

report() {
  local done=0 total=${#TARGETS[@]} times=() f
  for f in "${TARGETS[@]}"; do
    [ -f "$OUT/$f.png" ] && { done=$((done+1)); times+=("$(stat -c %Y "$OUT/$f.png")"); }
  done
  local now; now=$(date +%s)
  local start; start=$([ -f "$MARK" ] && stat -c %Y "$MARK" || echo "$now")
  local elapsed=$((now-start))
  printf '\r\033[K%2d/%d plates  elapsed %s' "$done" "$total" "$(hms $elapsed)"
  if [ "$done" -ge 2 ]; then
    local sorted first last rate rem eta
    sorted=$(printf '%s\n' "${times[@]}" | sort -n)
    first=$(echo "$sorted" | head -1); last=$(echo "$sorted" | tail -1)
    rate=$(( (last-first) / (done-1) ))
    rem=$((total-done))
    if [ "$rate" -gt 0 ] && [ "$rem" -gt 0 ]; then
      eta=$((rem*rate))
      printf '  rate %s/plate  ETA %s (IST %s)' "$(hms $rate)" "$(hms $eta)" \
        "$(date -d "@$((now+eta))" '+%H:%M')"
    fi
  elif [ "$done" -lt 2 ]; then
    printf '  no rate yet, needs two finished plates before an ETA means anything'
  fi
  [ "$done" -eq "$total" ] && { printf '  DONE\n'; return 1; }
  return 0
}

if [ "${1:-}" = "--watch" ]; then
  while report; do sleep 10; done
else
  report; echo
fi
