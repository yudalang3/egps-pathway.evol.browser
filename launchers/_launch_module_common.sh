#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd -- "$project_root"

fail() {
    printf 'ERROR: %s\n' "$1" >&2
    exit 1
}

if [[ -n "${JAVA_HOME:-}" ]]; then
    launch_java="$JAVA_HOME/bin/java"
    [[ -x "$launch_java" ]] || fail 'JAVA_HOME must point to a Linux JDK 25 or newer.'
else
    launch_java="$(command -v java)" || fail 'Java was not found. Set JAVA_HOME or add Linux JDK 25 to PATH.'
fi
java_settings="$("$launch_java" -XshowSettings:properties -version 2>&1)" || fail 'Unable to run Java. Check JAVA_HOME and PATH.'
java_version="$(printf '%s\n' "$java_settings" | sed -n 's/^[[:space:]]*java\.specification\.version = \([0-9][0-9]*\)[[:space:]]*$/\1/p')"
[[ "$java_version" =~ ^[0-9]+$ ]] || fail 'JDK 25 or newer is required.'
(( java_version >= 25 )) || fail 'JDK 25 or newer is required.'
[[ "$java_settings" != *'os.name = Windows'* ]] || fail 'Use Linux Java in WSL, or run the .bat entry on Windows.'

module_class="${1:-}"
[[ -n "$module_class" ]] || fail 'A module loader class is required. Use a launch_module_*.sh entry.'
module_file="${module_class//.//}"
[[ -f "out/production/egps-pathway.evol.browser/$module_file.class" ]] || fail 'Compiled module classes are missing. Run bash compile.sh in the project root first.'
compgen -G 'dependency-egps/egps-shell-*.jar' >/dev/null || fail 'Required egps-shell JAR is missing from dependency-egps.'
compgen -G 'dependency-egps/egps-base-*.jar' >/dev/null || fail 'Required egps-base JAR is missing from dependency-egps.'

# Match the portable options in eGPS.args, omitting Windows-only package exports.
jvm_options=(
    --add-exports java.desktop/sun.swing=ALL-UNNAMED
    --add-exports java.desktop/sun.awt.image=ALL-UNNAMED
    --add-exports java.desktop/sun.awt.shell=ALL-UNNAMED
    --add-opens java.desktop/java.awt=ALL-UNNAMED
    --add-opens java.desktop/java.awt.color=ALL-UNNAMED
    --add-opens java.desktop/javax.swing=ALL-UNNAMED
    --add-opens java.base/java.lang=ALL-UNNAMED
    --add-opens java.base/java.util=ALL-UNNAMED
    --add-opens java.base/java.text=ALL-UNNAMED
    --add-opens java.base/java.math=ALL-UNNAMED
    --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED
    --add-opens java.xml/com.sun.org.apache.xml.internal.serialize=ALL-UNNAMED
    -Dcom.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize
)
exec "$launch_java" -cp 'out/production/egps-pathway.evol.browser:dependency-egps/*' -Xmx12g "${jvm_options[@]}" egps2.Launcher "$module_class"
