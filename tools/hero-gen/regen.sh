#!/usr/bin/env bash
# Regenerate the hero plates from PROMPTS-hero.md.
#
#   tools/hero-gen/regen.sh              all fourteen
#   tools/hero-gen/regen.sh ledger depth just those
#   tools/hero-gen/regen.sh --list       what the prompt file defines
#
# This exists because the invocation is easy to get wrong in two ways that both
# fail confusingly.
#
# gen.mjs lives in the Axl tree and resolves --prompts and --out with
# path.join(ROOT, ...), and path.join does NOT treat an absolute second argument
# as absolute, it concatenates. An absolute --prompts path therefore becomes
# /home/dheirav/Code/Axl/mascot-source/home/dheirav/Code/... and fails. The
# paths below are relative to that ROOT, which is why they look odd: two levels
# up for --prompts and --out, three for the attachments inside the prompt file,
# because those resolve against ROOT/generated instead.
#
# And it has to run from the chatgpt-gen directory, with the signed-in browser
# already open, in a terminal you can type into: when ChatGPT changes its page
# the script stops and asks you to do that one step by hand.
set -eu

GEN=/home/dheirav/Code/Axl/tools/chatgpt-gen
PROMPTS=../../ThirstTrap/tools/hero-gen/PROMPTS-hero.md
OUT=../../ThirstTrap/tools/hero-gen/out
PORT=${AXL_CDP_PORT:-9222}

cd "$GEN"

if [ "${1:-}" = "--list" ]; then
  exec node gen.mjs --list --prompts "$PROMPTS" --out "$OUT"
fi

# Mirrored networking means a closed port hangs rather than refusing, so this
# needs the timeout or it waits forever.
if ! curl -s --max-time 3 "http://127.0.0.1:$PORT/json/version" >/dev/null 2>&1; then
  echo "No signed-in browser on port $PORT."
  echo "Open one in another terminal and leave it open:"
  echo "    $GEN/browser.sh            (or --login once, if you have never signed in)"
  exit 1
fi

if [ $# -eq 0 ]; then set -- --all; fi

# gen.mjs never overwrites: with ledger.png already there the new one lands as
# ledger-2.png. Nothing downstream looks at that name. check-plates.py would
# skip it, because "ledger-2" is not a shot, and encode.py would publish a
# site/hero/ledger-2.avif that no page loads while ledger.avif stayed old. The
# whole run would look like it worked and change nothing, so move the plates
# this run is about to replace out of the way first. Moved, not deleted: a
# regeneration can come back worse.
#
# Only the ones this run is about to replace. The first version swept the whole
# directory on every run, so asking for two scenes archived all thirteen and
# left the eleven good ones stranded in prev-, where encode.py cannot see them.
PLATES=/home/dheirav/Code/ThirstTrap/tools/hero-gen/out
if [ "${KEEP_OLD:-}" != "1" ]; then
  if [ "$1" = "--all" ]; then
    DOOMED=$(ls "$PLATES"/*.png 2>/dev/null || true)
  else
    DOOMED=$(for n in "$@"; do [ -f "$PLATES/$n.png" ] && echo "$PLATES/$n.png"; done)
  fi
  if [ -n "$DOOMED" ]; then
    PREV="$PLATES/prev-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$PREV"
    echo "$DOOMED" | while read -r f; do mv "$f" "$PREV"/; done
    echo "Moved $(ls "$PREV" | wc -l) plate(s) this run replaces to ${PREV#$PLATES/}/"
    echo "To put them back without clobbering anything new:"
    echo "    cd $PLATES && for f in $PREV/*.png; do [ -f \"\$(basename \"\$f\")\" ] || mv \"\$f\" .; done"
    echo
  fi
fi

echo "Watch it from another terminal with:"
echo "    $GEN/progress.sh --watch"
echo
node gen.mjs --prompts "$PROMPTS" --out "$OUT" "$@"

echo
echo "Now score the new plates against the room:"
echo "    python3 /home/dheirav/Code/ThirstTrap/tools/hero-gen/check-plates.py --src"
