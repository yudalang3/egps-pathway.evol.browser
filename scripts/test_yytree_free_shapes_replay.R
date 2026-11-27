# BLK-03 automated 3-layer clean-session replay acceptance for yytree free shapes.
#
# Pre-specified deterministic acceptance thresholds (fixed prior to evaluation):
#   - Layer 1 (Syntax & Clean-Session Execution): parse/eval in a fresh Rscript --vanilla
#     subprocess loading the installed yytree package, with RDS serialization isolation.
#   - Layer 2 (Object & Geometry Attribute Equivalence):
#       * numeric geometry tolerance: 1e-12
#       * snapped node logical coordinate tolerance: 1e-9
#       * exact layer count, type sequence (Z-order), colors, text, and heatmap RGBA cells
#   - Layer 3 (Rendered Output Pixel Equivalence):
#       * fresh-session renderPng vs Java exportSnapshot() reference PNG
#       * mean absolute RGB difference <= 1e-4, max per-channel pixel diff <= 2 (expected 0)
#       * Z-order overlap probe pixel matches expected top-layer color and flips vs control

GEOM_TOL <- 1e-12
SNAP_TOL <- 1e-9
PIXEL_MEAN_TOL <- 1e-4
PIXEL_MAX_TOL <- 2L

args <- commandArgs(trailingOnly = TRUE)
egps <- if (length(args) >= 1L && nzchar(args[1])) args[1] else Sys.getenv("EGPS_HOME")
if (!nzchar(egps)) stop("Pass eGPS2 installation path as argv[1] or set EGPS_HOME.", call. = FALSE)
lib_dir <- if (length(args) >= 2L && nzchar(args[2])) args[2] else {
  if (dir.exists("temp/yytree-library/yytree")) "temp/yytree-library"
  else if (dir.exists("rPackage/library/yytree")) "rPackage/library"
  else ""
}
python_bin <- if (length(args) >= 3L && nzchar(args[3])) args[3] else {
  if (nzchar(Sys.which("python3"))) "python3" else "python"
}
fixture_file <- if (length(args) >= 4L && nzchar(args[4])) args[4] else "temp/yytree-free-shapes/roundtrip.json"
report_prefix <- if (length(args) >= 5L && nzchar(args[5])) args[5] else "logs/yytree-free-shapes-replay"

if (!file.exists(fixture_file)) stop("Replay fixture file not found: ", fixture_file, call. = FALSE)
if (nzchar(lib_dir)) .libPaths(c(normalizePath(lib_dir, winslash = "/"), .libPaths()))

rscript_bin <- file.path(R.home("bin"), if (.Platform$OS.type == "windows") "Rscript.exe" else "Rscript")
out_dir <- normalizePath("temp/yytree-free-shapes/replay", winslash = "/", mustWork = FALSE)
dir.create(out_dir, recursive = TRUE, showWarnings = FALSE)
dir.create(dirname(report_prefix), recursive = TRUE, showWarnings = FALSE)

# Build a clean-session child worker script that runs ALL scenarios in a fresh Rscript --vanilla
# process with zero shared memory/editor state, using only the installed yytree package.
worker_script <- file.path(out_dir, "clean_replay_worker.R")
writeLines(c(
  'wargs <- commandArgs(trailingOnly = TRUE)',
  'egps <- wargs[1]',
  'lib_dir <- wargs[2]',
  'fixture_file <- wargs[3]',
  'out_dir <- wargs[4]',
  'if (nzchar(lib_dir)) .libPaths(c(normalizePath(lib_dir, winslash = "/"), .libPaths()))',
  'library(yytree)',
  'stopifnot(!"rJava" %in% loadedNamespaces())',
  'cfg <- config_yytree(egps)',
  'on.exit(quit_yytree(), add = TRUE)',
  'fixtures <- jsonlite::fromJSON(fixture_file, simplifyVector = FALSE)',
  'base_plot <- yytree("(A:1,B:2);", width = 1000, height = 800)',
  'base_serialized <- serialize(base_plot, NULL)',
  'results <- list()',
  'for (idx in seq_along(fixtures)) {',
  '  fx <- fixtures[[idx]]',
  '  name <- if (!is.null(fx$name)) fx$name else paste0("fixture_", idx)',
  '  code <- trimws(fx$code)',
  '  parsed <- parse(text = if (identical(code, "# No free shapes")) "base_plot" else paste("base_plot", code))',
  '  replayed <- eval(parsed)',
  '  stopifnot(inherits(replayed, "yytree"), identical(serialize(base_plot, NULL), base_serialized))',
  '  # Verify RDS serialization round-trip independence in the clean session.',
  '  rds_path <- file.path(out_dir, paste0(name, ".rds"))',
  '  saveRDS(replayed, rds_path)',
  '  restored <- readRDS(rds_path)',
  '  stopifnot(identical(yytree:::.scene_json(replayed), yytree:::.scene_json(restored)))',
  '  scene_json <- yytree:::.scene_json(replayed)',
  '  actual_layers <- jsonlite::fromJSON(scene_json, simplifyVector = FALSE)$layers',
  '  png_path <- file.path(out_dir, paste0("replay-", name, ".png"))',
  '  meta <- jsonlite::fromJSON(yytree:::.runtime_request("renderPng", scene_json, png_path))',
  '  stopifnot(!meta$desktopLaunched, file.exists(png_path), file.info(png_path)$size > 0)',
  '  results[[idx]] <- list(name = name, actual_layers = actual_layers, replay_png = png_path)',
  '}',
  'probe <- jsonlite::fromJSON(cfg$probe)',
  'payload <- list(',
  '  r_version = R.version.string,',
  '  os = paste(Sys.info()[["sysname"]], Sys.info()[["release"]], Sys.info()[["machine"]]),',
  '  java_version = probe$java,',
  '  java_arch = probe$architecture,',
  '  scenarios = results',
  ')',
  'jsonlite::write_json(payload, file.path(out_dir, "worker-output.json"), auto_unbox = TRUE, digits = NA, null = "null")'
), worker_script, useBytes = TRUE)

res <- processx::run(
  rscript_bin,
  c("--vanilla", worker_script, egps, lib_dir, normalizePath(fixture_file, winslash = "/"), out_dir),
  error_on_status = FALSE
)
if (res$status != 0L) {
  stop("Clean-session R replay worker failed (exit ", res$status, "):\n", res$stdout, "\n", res$stderr, call. = FALSE)
}

fixtures <- jsonlite::fromJSON(fixture_file, simplifyVector = FALSE)
worker_out <- jsonlite::fromJSON(file.path(out_dir, "worker-output.json"), simplifyVector = FALSE)
stopifnot(length(worker_out$scenarios) == length(fixtures))

to_logical_x <- function(layer, val) {
  switch(layer$mode, relative = val * 1000, percent = val * 10, pixel = val, stop("Unknown mode: ", layer$mode))
}
to_logical_y <- function(layer, val) {
  switch(layer$mode, relative = val * 800, percent = val * 8, pixel = val, stop("Unknown mode: ", layer$mode))
}

verify_layers <- function(actual, expected, name) {
  if (length(actual) != length(expected))
    stop(name, ": layer count mismatch (", length(actual), " vs ", length(expected), ")")
  for (i in seq_along(actual)) {
    exp_i <- expected[[i]]
    act_i <- actual[[i]]
    if (!identical(act_i$type, exp_i$type))
      stop(name, ": layer ", i, " type/Z-order mismatch (", act_i$type, " vs ", exp_i$type, ")")
    for (key in names(exp_i)) {
      if (key %in% c("source", "line_color", "fill_color") && is.null(exp_i[[key]])) next
      if (is.null(exp_i[[key]]) && !key %in% names(act_i)) next
      if (key == "cells") {
        if (!identical(act_i[[key]], exp_i[[key]]))
          stop(name, ": layer ", i, " heatmap cells mismatch")
      } else if (key == "source") {
        for (field in c("matrix", "colors", "limits")) {
          cmp <- all.equal(act_i$source[[field]], exp_i$source[[field]], tolerance = GEOM_TOL, check.attributes = FALSE)
          if (!isTRUE(cmp)) stop(name, ": layer ", i, " heatmap source$", field, " mismatch: ", paste(cmp, collapse = "; "))
        }
      } else {
        cmp <- all.equal(act_i[[key]], exp_i[[key]], tolerance = GEOM_TOL, check.attributes = FALSE)
        if (!isTRUE(cmp)) stop(name, ": layer ", i, " (", exp_i$type, ") field ", key, ": ", paste(cmp, collapse = "; "))
      }
    }
  }
}

scenario_reports <- list()
probe_colors <- list()

for (idx in seq_along(fixtures)) {
  fx <- fixtures[[idx]]
  sc <- worker_out$scenarios[[idx]]
  name <- sc$name
  actual_layers <- sc$actual_layers
  expected_layers <- fx$layers

  # Layer 2: Object & JSON geometry/order/snap/mode verification
  verify_layers(actual_layers, expected_layers, name)

  checks <- fx$checks
  snap_err <- NULL
  if (!is.null(checks$modes_expected)) {
    present_modes <- unique(vapply(actual_layers, function(l) l$mode, ""))
    if (!all(unlist(checks$modes_expected) %in% present_modes))
      stop(name, ": missing coordinate modes; present=", paste(present_modes, collapse = ","))
  }
  if (!is.null(checks$shape_types_expected)) {
    present_types <- unique(vapply(actual_layers, function(l) l$type, ""))
    if (!all(unlist(checks$shape_types_expected) %in% present_types))
      stop(name, ": missing shape types; present=", paste(present_types, collapse = ","))
  }
  if (!is.null(checks$snap_check)) {
    sc_info <- checks$snap_check
    layer <- actual_layers[[as.integer(sc_info$layer_index)]]
    ax <- to_logical_x(layer, layer[[sc_info$x_field]])
    ay <- to_logical_y(layer, layer[[sc_info$y_field]])
    dx <- abs(ax - sc_info$expected_x)
    dy <- abs(ay - sc_info$expected_y)
    snap_err <- list(dx = dx, dy = dy)
    if (dx > SNAP_TOL || dy > SNAP_TOL)
      stop(name, ": snapped endpoint error exceeded ", SNAP_TOL, " (dx=", dx, ", dy=", dy, ")")
  }
  if (isTRUE(checks$empty_list)) {
    stopifnot(length(actual_layers) == 0L, identical(trimws(fx$code), "# No free shapes"))
  }

  # Layer 3: Rendered PNG pixel comparison vs Java exportSnapshot() reference PNG
  ref_png <- fx$java_snapshot_png
  stopifnot(!is.null(ref_png), file.exists(ref_png))
  cmp_args <- c("scripts/compare_png_pixels.py", sc$replay_png, ref_png)
  if (!is.null(checks$zorder_probe)) {
    cmp_args <- c(cmp_args, as.character(checks$zorder_probe$x), as.character(checks$zorder_probe$y))
  }
  px_run <- processx::run(python_bin, cmp_args, error_on_status = FALSE)
  if (px_run$status != 0L) stop(name, ": pixel comparison failed:\n", px_run$stderr, call. = FALSE)
  px <- jsonlite::fromJSON(px_run$stdout, simplifyVector = TRUE)
  if (px$mean_abs_diff > PIXEL_MEAN_TOL || px$max_channel_diff > PIXEL_MAX_TOL) {
    stop(name, ": pixel diff exceeded tolerance (mean=", px$mean_abs_diff, ", max=", px$max_channel_diff, ")")
  }
  if (!is.null(checks$zorder_probe)) {
    expected_rgb <- as.integer(unlist(checks$zorder_probe$expected_rgb))
    actual_rgb <- as.integer(px$probe$actual_rgb)
    ref_rgb <- as.integer(px$probe$reference_rgb)
    if (max(abs(actual_rgb - expected_rgb)) > PIXEL_MAX_TOL || max(abs(actual_rgb - ref_rgb)) > PIXEL_MAX_TOL) {
      stop(name, ": Z-order probe RGB mismatch; actual=", paste(actual_rgb, collapse = ","),
           " expected=", paste(expected_rgb, collapse = ","), " ref=", paste(ref_rgb, collapse = ","))
    }
    probe_colors[[name]] <- actual_rgb
  }

  scenario_reports[[name]] <- list(
    layer_count = length(actual_layers),
    layer1_clean_session_exec = TRUE,
    layer2_object_tolerance_passed = TRUE,
    snap_error = snap_err,
    layer3_pixel_passed = TRUE,
    md5_equal = px$md5_equal,
    mean_abs_diff = px$mean_abs_diff,
    max_channel_diff = px$max_channel_diff,
    zorder_probe_rgb = if (!is.null(checks$zorder_probe)) as.integer(px$probe$actual_rgb) else NULL
  )
}

# Confirm Z-order flip between reordered main scene and pre-reorder control scene.
if ("nine_shapes_modes_snap_zorder" %in% names(probe_colors) && "zorder_before_reorder" %in% names(probe_colors)) {
  stopifnot(!identical(probe_colors$nine_shapes_modes_snap_zorder, probe_colors$zorder_before_reorder))
}

java_audit_path <- file.path(dirname(fixture_file), "java-audit.json")
java_audit <- if (file.exists(java_audit_path)) jsonlite::fromJSON(java_audit_path, simplifyVector = TRUE) else NULL
git_commit <- tryCatch(trimws(processx::run("git", c("rev-parse", "--short", "HEAD"))$stdout), error = function(e) "unknown")

report <- list(
  timestamp = format(Sys.time(), "%Y-%m-%dT%H:%M:%S%z"),
  git_commit = git_commit,
  os = worker_out$os,
  r_version = worker_out$r_version,
  java_version = worker_out$java_version,
  java_arch = worker_out$java_arch,
  thresholds = list(
    geometry_tolerance = GEOM_TOL,
    snap_tolerance = SNAP_TOL,
    pixel_mean_abs_diff_max = PIXEL_MEAN_TOL,
    pixel_max_channel_diff_max = PIXEL_MAX_TOL
  ),
  java_audit = java_audit,
  scenario_count = length(scenario_reports),
  all_passed = TRUE,
  scenarios = scenario_reports
)

json_path <- paste0(report_prefix, ".json")
log_path <- paste0(report_prefix, ".log")
jsonlite::write_json(report, json_path, auto_unbox = TRUE, pretty = TRUE, null = "null")

lines <- c(
  sprintf("BLK-03 Free-Shape 3-Layer Clean-Session Replay Acceptance (%s)", report$timestamp),
  sprintf("Commit: %s | OS: %s | R: %s | Java: %s (%s)", report$git_commit, report$os, report$r_version, report$java_version, report$java_arch),
  sprintf("Thresholds: geom<=%.0e, snap<=%.0e, pixel_mean<=%.0e, pixel_max<=%d", GEOM_TOL, SNAP_TOL, PIXEL_MEAN_TOL, PIXEL_MAX_TOL),
  sprintf("Scenarios passed: %d/%d", length(scenario_reports), length(scenario_reports))
)
for (nm in names(scenario_reports)) {
  r <- scenario_reports[[nm]]
  lines <- c(lines, sprintf("  [PASS] %-30s layers=%2d md5_equal=%-5s mean_diff=%.6g max_diff=%d",
    nm, r$layer_count, r$md5_equal, r$mean_abs_diff, r$max_channel_diff))
}
writeLines(lines, log_path, useBytes = TRUE)
cat(paste(lines, collapse = "\n"), "\n")
