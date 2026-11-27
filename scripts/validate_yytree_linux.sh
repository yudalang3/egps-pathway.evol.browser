#!/usr/bin/env bash
set -euo pipefail

# Cross-platform Linux validation runner for yytree (BLK-02 & BLK-03).
# Usage:
#   EGPS_HOME=/path/to/eGPS_v2_ubuntu_x64 bash scripts/validate_yytree_linux.sh
# Optional environment overrides:
#   R_BIN         (default: R on PATH)
#   RSCRIPT_BIN   (default: Rscript on PATH)
#   PYTHON_BIN    (default: python3 on PATH)
#   PDFTOPPM_BIN  (default: pdftoppm on PATH if available)
#   R_EXTRA_LIBS  (optional extra R library path, e.g. containing ape)
#   YYTREE_FONT_DIR (optional TrueType CJK font directory for iText 5.1.3 PDF embedding)

EGPS_HOME="${EGPS_HOME:-${1:-}}"
if [ -z "$EGPS_HOME" ] || [ ! -d "$EGPS_HOME" ]; then
  echo "ERROR: Set EGPS_HOME or pass the Linux eGPS2 directory as argv[1]." >&2
  exit 1
fi

R_BIN="${R_BIN:-$(command -v R || true)}"
RSCRIPT_BIN="${RSCRIPT_BIN:-$(command -v Rscript || true)}"
PYTHON_BIN="${PYTHON_BIN:-$(command -v python3 || true)}"
PDFTOPPM_BIN="${PDFTOPPM_BIN:-$(command -v pdftoppm || true)}"

if [ -z "$R_BIN" ] || [ -z "$RSCRIPT_BIN" ]; then
  echo "ERROR: R / Rscript not found. Set R_BIN and RSCRIPT_BIN or put them on PATH." >&2
  exit 1
fi
if [ -z "$PYTHON_BIN" ]; then
  echo "ERROR: python3 not found. Set PYTHON_BIN or put python3 on PATH." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

export LC_ALL="${LC_ALL:-C.UTF-8}"
export LANG="${LANG:-C.UTF-8}"
export EGPS_HOME
if [ -n "${R_EXTRA_LIBS:-}" ]; then
  export R_LIBS_USER="${R_EXTRA_LIBS}${R_LIBS_USER:+:$R_LIBS_USER}"
fi

WORK_DIR="$PROJECT_DIR/temp/yytree-linux-validation"
LIB_DIR="$WORK_DIR/library"
RELEASE_DIR="$WORK_DIR/release"
EXPORT_DIR="$WORK_DIR/exports"
LOG_DIR="$PROJECT_DIR/logs"
mkdir -p "$LIB_DIR" "$RELEASE_DIR" "$EXPORT_DIR" "$LOG_DIR"

LOG_FILE="$LOG_DIR/yytree-linux-validation.log"
: > "$LOG_FILE"
log() { echo "$*" | tee -a "$LOG_FILE"; }

log "=== yytree Linux Cross-Platform Validation ==="
log "Date: $(date -Iseconds)"
log "OS: $(uname -a)"
log "EGPS_HOME: $EGPS_HOME"
log "R: $("$RSCRIPT_BIN" --version 2>&1)"
log "Python: $("$PYTHON_BIN" --version 2>&1)"
log "pdftoppm: ${PDFTOPPM_BIN:-MISSING}"
log "YYTREE_FONT_DIR: ${YYTREE_FONT_DIR:-<unset>}"

log "--- 1. R CMD build & R CMD check ---"
(
  cd "$RELEASE_DIR"
  "$R_BIN" CMD build "$PROJECT_DIR/rPackage" 2>&1 | tee -a "$LOG_FILE"
  TARBALL="$(ls -1 yytree_*.tar.gz | head -n 1)"
  "$R_BIN" CMD check --no-manual --no-build-vignettes "$TARBALL" 2>&1 | tee -a "$LOG_FILE"
  cp -f yytree.Rcheck/00check.log "$LOG_DIR/yytree-linux-check.log"
  grep -q '^Status: OK$' "$LOG_DIR/yytree-linux-check.log"
)

TARBALL="$(ls -1 "$RELEASE_DIR"/yytree_*.tar.gz | head -n 1)"
log "--- 2. Clean-environment R CMD INSTALL ---"
"$R_BIN" CMD INSTALL --library="$LIB_DIR" "$TARBALL" 2>&1 | tee -a "$LOG_FILE"

log "--- 3. Package installation & SVG smoke test ---"
"$RSCRIPT_BIN" --vanilla scripts/test_yytree_binary.R "$TARBALL" "$EGPS_HOME" 2>&1 | tee -a "$LOG_FILE"

log "--- 4. Process lifecycle, audit, and bilingual examples ---"
mkdir -p "$PROJECT_DIR/temp/yytree-library"
"$R_BIN" CMD INSTALL --library="$PROJECT_DIR/temp/yytree-library" "$TARBALL" >/dev/null
"$RSCRIPT_BIN" --vanilla scripts/test_yytree_lifecycle.R "$EGPS_HOME" 2>&1 | tee -a "$LOG_FILE"
"$RSCRIPT_BIN" --vanilla scripts/test_yytree_audit.R "$EGPS_HOME" 2>&1 | tee -a "$LOG_FILE"
"$RSCRIPT_BIN" --vanilla scripts/test_yytree_examples.R "$EGPS_HOME" 2>&1 | tee -a "$LOG_FILE"

log "--- 5. Java headless tests (PdfExportTest + FreeShapeEditorTest) on Linux JRE ---"
JAVA_BIN="$EGPS_HOME/jre/bin/java"
JAVA_ARGS=$(grep -Ev '^\s*#|^\s*$|plaf\.windows|sun\.awt\.windows' "$EGPS_HOME/eGPS2.args" | sed -E 's/^(--add-(opens|exports)) /\1=/')
# shellcheck disable=SC2086
"$JAVA_BIN" $JAVA_ARGS -Djava.awt.headless=true \
  -cp "temp/yytree-build/tests:temp/yytree-build/classes:$EGPS_HOME/dependency-egps/*" \
  api.rpython.yytree.PdfExportTest 2>&1 | tee -a "$LOG_FILE"
# shellcheck disable=SC2086
"$JAVA_BIN" $JAVA_ARGS -Djava.awt.headless=true \
  -cp "temp/yytree-build/tests:temp/yytree-build/classes:$EGPS_HOME/dependency-egps/*" \
  api.rpython.yytree.FreeShapeEditorTest 2>&1 | tee -a "$LOG_FILE"

log "--- 6. BLK-03 3-layer clean-session free-shape replay on Linux ---"
"$RSCRIPT_BIN" --vanilla scripts/test_yytree_free_shapes_replay.R \
  "$EGPS_HOME" "$LIB_DIR" "$PYTHON_BIN" \
  "temp/yytree-free-shapes/roundtrip.json" \
  "logs/yytree-free-shapes-replay-linux" 2>&1 | tee -a "$LOG_FILE"

log "--- 7. 10-case PDF/SVG/PPTX vector export & Unicode/CJK verification on Linux ---"
"$RSCRIPT_BIN" --vanilla scripts/test_yytree_pdf_vector.R "$EGPS_HOME" "$EXPORT_DIR" "$LIB_DIR" 2>&1 | tee -a "$LOG_FILE"
EXTRA_PIXEL_ARGS=()
if [ -n "$PDFTOPPM_BIN" ]; then
  EXTRA_PIXEL_ARGS=(--pdftoppm "$PDFTOPPM_BIN" --require-pdf-pixels)
fi
"$PYTHON_BIN" scripts/check_yytree_exports_structural.py \
  --dir "$EXPORT_DIR" \
  --report "$LOG_DIR/yytree-linux-exports.json" \
  "${EXTRA_PIXEL_ARGS[@]}" 2>&1 | tee -a "$LOG_FILE"

log "=== Linux validation completed successfully ==="
