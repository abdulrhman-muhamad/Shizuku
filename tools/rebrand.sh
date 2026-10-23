#!/usr/bin/env bash
# Replace the user-visible "Shizuku" product name with "WhtsAgent Helper" in every
# translated strings.xml and in the rish launcher script.
#
# Idempotent: once applied there is nothing left to match, so re-running is a no-op.
# After an upstream merge that conflicts in res/values*/strings.xml, take upstream's
# version of those files and re-run this script (see plan/02-sync-with-upstream.md).
#
# Deliberately NOT touched: resource IDs (lowercase, e.g. toast_shizuku_already_starting),
# style names, Kotlin/Java packages, and the binder contract strings
# (moe.shizuku.manager.permission.*, moe.shizuku.privileged.api.intent.extra.BINDER) —
# clients have those compiled in.
#
# Inflected forms are mapped per language before the plain replacement, because keeping
# the original suffix is wrong for e.g. Finnish (Shizukun -> Helperin, not Helpern).
set -euo pipefail
cd "$(dirname "$0")/.."

BRAND='WhtsAgent Helper'
RES=manager/src/main/res

rep() { # rep <file> <perl substitution body...>
  local f=$1; shift
  [ -f "$f" ] || return 0
  BRAND="$BRAND" perl -CSD -Mutf8 -pi -e "$@" "$f"
}

# --- Language-specific inflections (must run before the generic pass) -------------------
# Finnish: genitive -n, partitive -a, illative -un
rep "$RES/values-fi/strings.xml" '
  s/Shizukuun\b/$ENV{BRAND}iin/g; s/Shizukun\b/$ENV{BRAND}in/g; s/Shizukua\b/$ENV{BRAND}ia/g;'
# Hungarian: accusative -t, dative -nak/-nek, illative -ba/-be (front vowel harmony for "Helper")
rep "$RES/values-hu/strings.xml" '
  s/Shizukunak\b/$ENV{BRAND}nek/g; s/Shizukuba\b/$ENV{BRAND}be/g; s/Shizukut\b/$ENV{BRAND}t/g;'
# Azerbaijani: ablative, accusative, dative — hyphenated onto the foreign name
rep "$RES/values-az/strings.xml" '
  s/Shizukudan\b/$ENV{BRAND}-dən/g; s/Shizukunu\b/$ENV{BRAND}-i/g; s/Shizukuya\b/$ENV{BRAND}-ə/g;'
# Arabic: transliteration, including the attached preposition li-
rep "$RES/values-ar/strings.xml" '
  s/لشيزوكو/لـ$ENV{BRAND}/g; s/شيزوكو/$ENV{BRAND}/g;'
# Serbian (Cyrillic): keep the Cyrillic case ending, hyphenated
for f in "$RES"/values-sr*/strings.xml; do
  rep "$f" 's/Шизуку-у/$ENV{BRAND}-у/g; s/Шизукуа/$ENV{BRAND}-а/g; s/Шизукуу/$ENV{BRAND}-у/g; s/Шизуку/$ENV{BRAND}/g;'
done
# Russian / Ukrainian
for l in ru uk; do rep "$RES/values-$l/strings.xml" 's/Шизуку/$ENV{BRAND}/g;'; done
# Korean: 시즈쿠 and 헬퍼 both end in a vowel, so attached particles stay correct
rep "$RES/values-ko/strings.xml" 's/시즈쿠/$ENV{BRAND}/g;'
# Thai has no inter-word spaces; pad the Latin name where it touches Thai script
rep "$RES/values-th/strings.xml" '
  s/(?<=\p{Thai})ชิซูกุ/ ชิซูกุ/g; s/ชิซูกุ(?=\p{Thai})/ชิซูกุ /g; s/ชิซูกุ/$ENV{BRAND}/g;'
# Japanese / Traditional Chinese render the name as the kanji 雫 ("shizuku")
for l in ja zh-rTW; do rep "$RES/values-$l/strings.xml" 's/雫/$ENV{BRAND}/g;'; done

# --- Generic pass: every other occurrence of the Latin name ---------------------------
# Safe as a bare substring: the only letter ever preceding it is the n of a "\n" escape,
# and German/Nordic genitive -s and Slovenian -ja/-jev suffixes read correctly on "Helper".
for f in "$RES"/values*/strings.xml; do
  rep "$f" 's/Shizuku/$ENV{BRAND}/g;'
done

rep manager/src/main/assets/rish 's/in Shizuku app/in the $ENV{BRAND} app/g;'

echo "Remaining user-visible occurrences (expect none):"
grep -rnE 'Shizuku|شيزوك|Шизук|시즈쿠|ชิซูกุ|雫' "$RES"/values*/strings.xml manager/src/main/assets/rish | grep -v "rikka.shizuku.shell.ShizukuShellLoader" || echo "  none"
