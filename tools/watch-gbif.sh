#!/usr/bin/env bash
# Progress for the GBIF resolve, ETA from the measured rate.
cd "$(dirname "$0")"; start=$(date +%s); first=""
while :; do
  p=$(cat gbif_progress.txt 2>/dev/null || echo "starting...")
  case "$p" in DONE*) echo; echo "$p"; break;; esac
  n=$(echo "$p" | grep -o '^[0-9]*'); tot=$(echo "$p" | grep -o '/[0-9]*' | head -1 | tr -d /)
  now=$(date +%s); el=$((now-start)); [ -z "$first" ] && first=$n
  if [ -n "$n" ] && [ -n "$tot" ] && [ "$el" -gt 5 ] && [ "${n:-0}" -gt "${first:-0}" ]; then
    eta=$(awk "BEGIN{r=($n-$first)/$el; if(r>0) printf \"%ds\", ($tot-$n)/r; else printf \"-\"}")
    printf '\r%s | elapsed %ds | ETA %s   ' "$p" "$el" "$eta"
  else
    printf '\r%s | elapsed %ds | ETA -   ' "$p" "$el"
  fi
  sleep 2
done
