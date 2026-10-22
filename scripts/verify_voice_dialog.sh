#!/usr/bin/env bash
set -euo pipefail

BROWSER_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SHELL_ROOT="$(cd "$BROWSER_ROOT/../egps-shell" && pwd)"
mkdir -p "$BROWSER_ROOT/temp" "$BROWSER_ROOT/logs"
RUN_DIR="$(mktemp -d "$BROWSER_ROOT/temp/voice-dialog-XXXXXX")"
exec > >(tee "$BROWSER_ROOT/logs/voice-dialog-${1:-source}.log") 2>&1

build_project() {
    local root="$1" output="$2" classpath="$3" sources="$4"
    # Verify the absolute target before clearing a production output directory.
    if [[ "$(realpath -m "$output")" != "$root/out/production/$(basename "$output")" ]]; then
        printf 'Unexpected build output: %s\n' "$output" >&2
        exit 1
    fi
    rm -rf -- "$output"
    mkdir -p "$output"
    find "$root/src" -path "$root/src/test" -prune -o -name '*.java' -print |
        sed 's/.*/"&"/' > "$sources"
    javac -encoding UTF-8 -d "$output" -cp "$classpath" "@$sources"
    while IFS= read -r -d '' source; do
        local target="$output/${source#"$root/src/"}"
        mkdir -p "$(dirname "$target")"
        cp -- "$source" "$target"
    done < <(find "$root/src" -path "$root/src/test" -prune -o -type f ! -name '*.java' -print0)
}

SHELL_MAIN="$SHELL_ROOT/out/production/egps-shell"
SHELL_TEST="$SHELL_ROOT/out/test-classes"
BROWSER_MAIN="$BROWSER_ROOT/out/production/egps-pathway.evol.browser"
BROWSER_TEST="$BROWSER_ROOT/out/test-classes"
mkdir -p "$SHELL_TEST" "$BROWSER_TEST"
javac -version

if [[ "${1:-}" == deployed ]]; then
    SHELL_CP="$BROWSER_ROOT/dependency-egps/*"
else
    build_project "$SHELL_ROOT" "$SHELL_MAIN" "$SHELL_ROOT/dependency-egps/*" "$RUN_DIR/shell-sources.txt"
    SHELL_CP="$SHELL_MAIN:$SHELL_ROOT/dependency-egps/*"
fi

javac -encoding UTF-8 -d "$SHELL_TEST" -cp "$SHELL_CP" \
    "$SHELL_ROOT/src/test/egps2/builtin/modules/voice/VoiceDialogExampleLifecycleTest.java" \
    "$SHELL_ROOT/src/test/egps2/builtin/modules/voice/BookmarkDisplayPanelExamplesTest.java"
java -Djava.awt.headless=true "-Duser.home=$RUN_DIR/home-shell" -cp "$SHELL_TEST:$SHELL_CP" \
    egps2.builtin.modules.voice.VoiceDialogExampleLifecycleTest
java -Djava.awt.headless=true "-Duser.home=$RUN_DIR/home-shell" -cp "$SHELL_TEST:$SHELL_CP" \
    egps2.builtin.modules.voice.BookmarkDisplayPanelExamplesTest

BROWSER_CP="$BROWSER_ROOT/dependency-egps/*"
if [[ "${1:-}" != deployed ]]; then
    BROWSER_CP="$SHELL_MAIN:$BROWSER_CP"
    build_project "$BROWSER_ROOT" "$BROWSER_MAIN" "$BROWSER_CP" "$RUN_DIR/browser-sources.txt"
fi
javac -encoding UTF-8 -d "$BROWSER_TEST" -cp "$BROWSER_MAIN:$BROWSER_CP" \
    "$BROWSER_ROOT/src/test/module/evolview/moderntreeviewer/ModernTreeExamplesTest.java" \
    "$BROWSER_ROOT/src/test/module/evolview/moderntreeviewer/ModernTreeVoiceDialogRegressionTest.java"
cd "$BROWSER_ROOT"
java -Djava.awt.headless=true "-Duser.home=$RUN_DIR/home-examples" -cp "$BROWSER_TEST:$BROWSER_MAIN:$BROWSER_CP" \
    module.evolview.moderntreeviewer.ModernTreeExamplesTest
java -Djava.awt.headless=true "-Duser.home=$RUN_DIR/home-dialog" -cp "$BROWSER_TEST:$BROWSER_MAIN:$BROWSER_CP" \
    module.evolview.moderntreeviewer.ModernTreeVoiceDialogRegressionTest
printf 'VOICE verification passed (%s).\n' "${1:-source}"
