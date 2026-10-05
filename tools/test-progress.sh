#!/usr/bin/env bash
# Watch a Gradle test run by counting the testcase entries Gradle writes as it
# goes. Gradle prints task names, not a count, so this reads the XML results
# directory instead. The total is not known up front, so no ETA is offered:
# a guessed one would be worse than none.
set -u
R=${1:-/home/dheirav/Code/ThirstTrap}; W=${2:-}
start=$(cat /tmp/test.start 2>/dev/null || date +%s)
show() {
  local n el
  n=$(grep -ho 'tests="[0-9]*"' $(find "$R" -path '*/build/test-results/*' -name 'TEST-*.xml' 2>/dev/null) 2>/dev/null \
      | sed 's/[^0-9]//g' | awk '{s+=$1} END{print s+0}')
  el=$(( $(date +%s) - start ))
  printf '\r%s%5d tests done  elapsed %dm%02ds  ' \
    "$(grep -qE 'BUILD SUCCESSFUL|BUILD FAILED' /tmp/test.log 2>/dev/null && echo 'DONE ' || echo '')" \
    "$n" $((el/60)) $((el%60))
  [ "$n" -gt 0 ] && printf '%.1f tests/s   ' "$(awk -v n="$n" -v e="$el" 'BEGIN{print (e>0)?n/e:0}')"
}
if [ "$W" = "--watch" ]; then
  while :; do show; grep -qE 'BUILD SUCCESSFUL|BUILD FAILED' /tmp/test.log 2>/dev/null && { echo; break; }; sleep 3; done
  grep -E 'BUILD SUCCESSFUL|BUILD FAILED' /tmp/test.log
else show; echo; fi
