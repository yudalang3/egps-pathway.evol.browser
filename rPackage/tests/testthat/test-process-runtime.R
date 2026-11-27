test_that("large and partial protocol messages round trip without truncation", {
  logfile <- tempfile()
  process <- processx::process$new(file.path(R.home("bin"), if (.Platform$OS.type == "windows") "Rscript.exe" else "Rscript"),
    c("--vanilla", test_path("fixtures", "process-server.R"), "large"),
    stdin = "|", stdout = "|", stderr = logfile, encoding = "UTF-8",
    windows_hide_window = TRUE)
  on.exit(process$kill(), add = TRUE)
  payload <- paste(rep("input \u6811 ", 30000), collapse = "")
  result <- yytree:::.runtime_exchange(process, "echo", list(payload))
  expect_identical(result, paste(rep("Unicode \u6811 \u53f6 ", 30000), collapse = ""))
  expect_true(process$is_alive())
})

test_that("timeout, mismatched IDs and mid-request exit stop the transport", {
  for (mode in c("timeout", "mismatch", "exit")) {
    process <- processx::process$new(file.path(R.home("bin"), if (.Platform$OS.type == "windows") "Rscript.exe" else "Rscript"),
      c("--vanilla", test_path("fixtures", "process-server.R"), mode),
      stdin = "|", stdout = "|", stderr = tempfile(), encoding = "UTF-8",
      windows_hide_window = TRUE)
    tryCatch({
      message <- if (mode == "timeout") "timed out" else if (mode == "mismatch") "protocol response" else "exited before replying"
      expect_error(yytree:::.runtime_exchange(process, "echo", list("input"),
                   timeout = if (mode == "timeout") .5 else 10), message)
      expect_false(process$is_alive())
    }, finally = process$kill())
  }
})

test_that("interrupts terminate an in-flight process without retrying", {
  killed <- FALSE
  writes <- 0
  process <- list(write_input = function(bytes) {
    writes <<- writes + 1
    stop(structure(list(message = "test interrupt", call = NULL), class = c("interrupt", "condition")))
  }, kill = function() killed <<- TRUE)
  interrupted <- tryCatch(yytree:::.runtime_exchange(process, "echo"),
                          interrupt = function(e) TRUE)
  expect_true(interrupted)
  expect_true(killed)
  expect_identical(writes, 1)
})

test_that("quit is idempotent and deliberate quit requires config before printing", {
  quit_yytree()
  expect_invisible(quit_yytree())
  expect_false(yytree:::.runtime_alive())
  expect_error(print(yytree("(A:1,B:2);")), "config_yytree")
  expect_error(config_yytree(".", restart = NA), "restart must")
  expect_error(config_yytree(".", max_memory_heap = "0g"), "max_memory_heap must")
  expect_error(config_yytree(".", max_memory_stack = "bad"), "max_memory_stack must")
  expect_error(config_yytree(".", max_memory = "4g"), "matches multiple")
})

test_that("finalization stops its own runtime and leaves the current one alive", {
  make_process <- function() {
    state <- new.env()
    state$alive <- TRUE
    list(state = state, is_alive = function() state$alive,
         kill = function() state$alive <- FALSE,
         write_input = function(bytes) stop("closed transport"),
         get_error_file = function() NULL, wait = function(ms) NULL)
  }
  runtime <- yytree:::.runtime
  old <- new.env(parent = emptyenv())
  old$next_id <- 0
  old$process <- make_process()
  previous <- old$process
  current <- make_process()
  runtime$process <- current
  on.exit(quit_yytree(), add = TRUE)
  yytree:::.runtime_finalize(old)
  expect_false(previous$is_alive())
  expect_false(exists("process", old, inherits = FALSE))
  expect_true(current$is_alive())
})

test_that("interrupting the shutdown wait still clears a deliberate quit", {
  state <- new.env()
  state$alive <- TRUE
  runtime <- yytree:::.runtime
  runtime$config <- list()
  runtime$launch <- list()
  runtime$process <- list(
    is_alive = function() state$alive,
    write_input = function(bytes) {
      state$id <- jsonlite::fromJSON(rawToChar(bytes))$id
      raw(0)
    },
    poll_io = function(ms) NULL,
    read_output = function() paste0(jsonlite::toJSON(list(id = state$id, ok = TRUE), auto_unbox = TRUE), "\n"),
    wait = function(ms) stop(structure(list(message = "interrupt shutdown"), class = c("interrupt", "condition"))),
    kill = function() state$alive <- FALSE)
  on.exit(quit_yytree(), add = TRUE)
  interrupted <- tryCatch(quit_yytree(), interrupt = function(e) TRUE)
  expect_true(interrupted)
  expect_false(state$alive)
  expect_false(any(c("process", "config", "launch") %in% ls(runtime)))
})

test_that("quit forcibly terminates an unresponsive subprocess after its grace period", {
  process <- processx::process$new(file.path(R.home("bin"), if (.Platform$OS.type == "windows") "Rscript.exe" else "Rscript"),
    c("--vanilla", test_path("fixtures", "process-server.R"), "timeout"),
    stdin = "|", stdout = "|", stderr = tempfile(), encoding = "UTF-8",
    windows_hide_window = TRUE)
  on.exit(process$kill(), add = TRUE)
  runtime <- yytree:::.runtime
  runtime$process <- process
  on.exit(quit_yytree(), add = TRUE)
  elapsed <- system.time(quit_yytree())[["elapsed"]]
  expect_false(process$is_alive())
  expect_lt(elapsed, 3)
})

test_that("cross-platform eGPS root and Java resolvers support Windows, Linux, and macOS .app bundles", {
  base <- tempfile("egps-layouts-")
  dir.create(base, recursive = TRUE)
  on.exit(unlink(base, recursive = TRUE, force = TRUE), add = TRUE)

  # Standard Windows / Linux layout
  std <- file.path(base, "std")
  dir.create(file.path(std, "dependency-egps"), recursive = TRUE)
  dir.create(file.path(std, "jre", "bin"), recursive = TRUE)
  file.create(file.path(std, "jre", "bin", "java.exe"))
  file.create(file.path(std, "jre", "bin", "java"))
  if (.Platform$OS.type == "unix") Sys.chmod(file.path(std, "jre", "bin", "java"), "0755")
  expect_identical(yytree:::.resolve_egps_root(std), normalizePath(std, winslash = "/"))
  win_java <- yytree:::.resolve_java_runtime(std, java_home = "", os_type = "windows")
  expect_identical(win_java$executable, file.path(normalizePath(file.path(std, "jre"), winslash = "/"), "bin", "java.exe"))
  lin_java <- yytree:::.resolve_java_runtime(std, java_home = "", os_type = "unix")
  expect_identical(lin_java$executable, file.path(normalizePath(file.path(std, "jre"), winslash = "/"), "bin", "java"))

  # macOS .app bundle layout (both outer archive folder, .app bundle, and Contents/Resources)
  mac_outer <- file.path(base, "eGPS_v2_macOS_AArch64")
  mac_app <- file.path(mac_outer, "eGPS_v2_macOS.app")
  mac_res <- file.path(mac_app, "Contents", "Resources")
  dir.create(file.path(mac_res, "dependency-egps"), recursive = TRUE)
  dir.create(file.path(mac_res, "jre", "Contents", "Home", "bin"), recursive = TRUE)
  mac_java_bin <- file.path(mac_res, "jre", "Contents", "Home", "bin", "java")
  file.create(mac_java_bin)
  if (.Platform$OS.type == "unix") Sys.chmod(mac_java_bin, "0755")
  expected_res <- normalizePath(mac_res, winslash = "/")
  expect_identical(yytree:::.resolve_egps_root(mac_outer), expected_res)
  expect_identical(yytree:::.resolve_egps_root(mac_app), expected_res)
  expect_identical(yytree:::.resolve_egps_root(mac_res), expected_res)
  mac_java <- yytree:::.resolve_java_runtime(expected_res, java_home = "", os_type = "unix")
  expect_identical(mac_java$java, normalizePath(file.path(mac_res, "jre", "Contents", "Home"), winslash = "/"))
  expect_identical(mac_java$executable, file.path(mac_java$java, "bin", "java"))

  # JAVA_HOME fallback (including macOS .jdk/Contents/Home)
  no_jre <- file.path(base, "no-jre")
  dir.create(file.path(no_jre, "dependency-egps"), recursive = TRUE)
  jdk_bundle <- file.path(base, "temurin-25.jdk")
  dir.create(file.path(jdk_bundle, "Contents", "Home", "bin"), recursive = TRUE)
  jdk_bin <- file.path(jdk_bundle, "Contents", "Home", "bin", "java")
  file.create(jdk_bin)
  if (.Platform$OS.type == "unix") Sys.chmod(jdk_bin, "0755")
  fallback_java <- yytree:::.resolve_java_runtime(no_jre, java_home = jdk_bundle, os_type = "unix")
  expect_identical(fallback_java$java, normalizePath(file.path(jdk_bundle, "Contents", "Home"), winslash = "/"))
  expect_error(yytree:::.resolve_java_runtime(no_jre, java_home = "", os_type = "unix"), "No bundled Java")

  # Non-Windows JVM args filter out Windows-only java.desktop exports
  writeLines(c(
    "# comment",
    "--add-exports java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED",
    "--add-exports java.desktop/sun.awt.windows=ALL-UNNAMED",
    "--add-exports java.desktop/sun.swing=ALL-UNNAMED",
    "--add-opens java.desktop/java.awt=ALL-UNNAMED",
    "-Xmx2g",
    "-Dcom.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize"
  ), file.path(std, "eGPS2.args"))
  win_args <- yytree:::.read_egps_jvm_args(std, os_type = "windows")
  unix_args <- yytree:::.read_egps_jvm_args(std, os_type = "unix")
  expect_true(any(grepl("sun.awt.windows", win_args, fixed = TRUE)))
  expect_false(any(grepl("sun.awt.windows", unix_args, fixed = TRUE)))
  expect_false(any(grepl("plaf.windows", unix_args, fixed = TRUE)))
  expect_true("--add-exports=java.desktop/sun.swing=ALL-UNNAMED" %in% unix_args)
  expect_true("--add-opens=java.desktop/java.awt=ALL-UNNAMED" %in% unix_args)
  expect_false(any(grepl("^-Xmx", unix_args)))
})
