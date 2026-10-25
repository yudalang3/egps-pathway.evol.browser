# Run against the installed package; workers also verify R exit/crash cleanup.
args <- commandArgs(trailingOnly = TRUE)
.libPaths(c(normalizePath("temp/yytree-library", winslash = "/"), .libPaths()))
library(yytree)

if (startsWith(args[1], "--worker-")) {
  config <- config_yytree(args[2])
  if ("--gui" %in% args) print(yytree("(A:1,B:2);") + leaf_labels())
  jsonlite::write_json(list(java = config$pid), paste0(args[3], ".tmp"), auto_unbox = TRUE)
  stopifnot(file.rename(paste0(args[3], ".tmp"), args[3]))
  if (args[1] == "--worker-crash") Sys.sleep(60)
  if (args[1] == "--worker-unload") unloadNamespace("yytree")
  quit(save = "no")
}

main <- function() {
  egps <- args[1]
  gui <- "--gui" %in% args
  on.exit(quit_yytree(), add = TRUE)
  output <- file.path(getwd(), "temp", "yytree-lifecycle")
  dir.create(output, recursive = TRUE, showWarnings = FALSE)
  logs <- file.path(getwd(), "logs")
  dir.create(logs, showWarnings = FALSE)
  failure <- function(code, pattern) {
    message <- tryCatch({ force(code); stop("Expected failure was not raised.") }, error = conditionMessage)
    stopifnot(grepl(pattern, message, fixed = TRUE))
  }
  running <- function(pid) tryCatch(ps::ps_is_running(ps::ps_handle(pid)), error = function(e) FALSE)
  configuration <- config_yytree(egps)
  process <- yytree:::.runtime$process
  pid <- process$get_pid()
  stopifnot(pid != Sys.getpid(), identical(config_yytree(egps), configuration), !"rJava" %in% loadedNamespaces())
  stopifnot(configuration$max_memory_heap == "4g", configuration$max_memory_stack == "8m",
            all(c("-Xmx4g", "-Xss8m") %in% jsonlite::fromJSON(configuration$probe)$jvmArguments))
  failure(config_yytree(egps, max_memory_heap = "2g"), "restart = TRUE")
  failure(config_yytree(egps, max_memory_stack = "16m"), "restart = TRUE")
  stopifnot(process$is_alive())
  failure(config_yytree(file.path(output, "missing")), "path")
  empty <- file.path(output, "empty")
  dir.create(empty, showWarnings = FALSE)
  failure(config_yytree(empty), "No dependency-egps JARs")
  failure(yytree:::.runtime_request("not-a-command"), "Unknown command")
  failure(yytree:::.runtime_request("renderPng", "{}", file.path(output, "invalid.png")), "failed")
  stopifnot(process$is_alive(), process$get_pid() == pid)

  # A real rendering request larger than the OS pipe buffer, with UTF-8 paths/text.
  set.seed(21)
  large <- yytree(ape::rtree(1000), width = 1600, height = 1000) +
    leaf_annotation_heatmap(matrix(rnorm(20000), 1000, dimnames = list(paste0("t", 1:1000), paste0("V", 1:20))), legend = "none") +
    free_text(.1, .1, "\u6811 \u53f6 Unicode")
  payload <- yytree:::.scene_json(large)
  stopifnot(nchar(payload, type = "bytes") > 65536)
  path <- file.path(output, "\u4e2d\u6587 \u7a7a\u683c.png")
  meta <- jsonlite::fromJSON(yytree:::.runtime_request("renderPng", payload, path))
  stopifnot(meta$leaves == 1000, file.info(path)$size > 0, process$get_pid() == pid)
  scene <- yytree("(A:1,B:2);") + leaf_labels()
  original <- serialize(scene, NULL)
  if (gui) {
    stopifnot(identical(print(scene), scene))
    id <- yytree:::.runtime_request("showPlot", yytree:::.scene_json(scene), FALSE)
    stopifnot(identical(id, yytree:::.runtime_request("showPlot", yytree:::.scene_json(scene), FALSE)))
    yytree:::.runtime_request("closeAll")
    stopifnot(process$is_alive(), process$get_pid() == pid)
    reopened <- yytree:::.runtime_request("showPlot", yytree:::.scene_json(scene), FALSE)
    stopifnot(reopened != id, process$get_pid() == pid)
  }
  quit_yytree()
  stopifnot(!process$is_alive())
  quit_yytree()
  failure(print(scene), "config_yytree")
  # Exercise a real classpath under a non-ASCII installation directory.
  unicode_home <- file.path(output, "\u4e2d\u6587 eGPS")
  dependencies <- file.path(unicode_home, "dependency-egps")
  dir.create(dependencies, recursive = TRUE, showWarnings = FALSE)
  stopifnot(all(file.copy(list.files(file.path(egps, "dependency-egps"), "\\.jar$", full.names = TRUE), dependencies, overwrite = TRUE)))
  previous_java <- Sys.getenv("JAVA_HOME", unset = NA_character_)
  on.exit(if (is.na(previous_java)) Sys.unsetenv("JAVA_HOME") else Sys.setenv(JAVA_HOME = previous_java), add = TRUE)
  Sys.setenv(JAVA_HOME = configuration$java)
  writeLines(c(readLines(file.path(egps, "eGPS2.args"), warn = FALSE),
    "-Xmxinvalid", "-Xssinvalid", "-XX:MaxHeapSize=1", "-XX:ThreadStackSize=1"),
    file.path(unicode_home, "eGPS2.args"))
  unicode_config <- config_yytree(unicode_home)
  stopifnot(jsonlite::fromJSON(unicode_config$probe)$pid == unicode_config$pid,
            Sys.getenv("JAVA_HOME") == configuration$java)
  java_args <- jsonlite::fromJSON(unicode_config$probe)$jvmArguments
  stopifnot(sum(startsWith(java_args, "-Xmx")) == 1, sum(startsWith(java_args, "-Xss")) == 1,
            all(c("-Xmx4g", "-Xss8m") %in% java_args),
            !any(grepl("^-XX:(MaxHeapSize|ThreadStackSize)=", java_args)))
  quit_yytree()
  # Startup failures must report diagnostics and leave no usable process.
  previous_options <- Sys.getenv("JAVA_TOOL_OPTIONS", unset = NA_character_)
  on.exit(if (is.na(previous_options)) Sys.unsetenv("JAVA_TOOL_OPTIONS") else Sys.setenv(JAVA_TOOL_OPTIONS = previous_options), add = TRUE)
  Sys.setenv(JAVA_TOOL_OPTIONS = "-XX:NotARealYyTreeOption")
  failure(config_yytree(egps), "Cannot initialize yytree")
  stopifnot(!yytree:::.runtime_alive())
  if (is.na(previous_options)) Sys.unsetenv("JAVA_TOOL_OPTIONS") else Sys.setenv(JAVA_TOOL_OPTIONS = previous_options)
  configuration <- config_yytree(egps, max_memory_heap = "512m")
  process <- yytree:::.runtime$process
  restarted <- config_yytree(egps, max_memory_heap = "768m", max_memory_stack = "16m", restart = TRUE)
  stopifnot(!process$is_alive(), restarted$pid != configuration$pid)
  stopifnot(abs(jsonlite::fromJSON(restarted$probe)$maxMemory - 768 * 1024^2) < 768 * 1024^2 * .05)
  stopifnot("-Xss16m" %in% jsonlite::fromJSON(restarted$probe)$jvmArguments)
  process <- yytree:::.runtime$process
  process$kill()
  # Recovery must retain the resolved Java executable, even if R's environment changes.
  Sys.setenv(JAVA_HOME = file.path(output, "not-a-java-home"))
  recovered <- FALSE
  withCallingHandlers({
    if (gui) print(scene) else yytree:::.runtime_request("probeRuntime")
  }, warning = function(w) {
    stopifnot(grepl("Java restarted", conditionMessage(w), fixed = TRUE))
    recovered <<- TRUE
    invokeRestart("muffleWarning")
  })
  stopifnot(recovered, yytree:::.runtime$process$get_pid() != process$get_pid(),
            identical(serialize(scene, NULL), original), yytree:::.runtime$config$max_memory_stack == "16m",
            "-Xss16m" %in% jsonlite::fromJSON(yytree:::.runtime$config$probe)$jvmArguments)
  process <- yytree:::.runtime$process
  # A deadline shorter than one poll deterministically invalidates the session.
  failure(yytree:::.runtime_request("renderPng", payload, path, timeout = .001), "timed out")
  stopifnot(!process$is_alive(), identical(serialize(scene, NULL), original))
  suppressWarnings(yytree:::.runtime_request("probeRuntime"))
  quit_yytree()

  # Normal exit, forced R termination and namespace unload all release Java.
  for (mode in c("normal", "crash", "unload")) {
    ready <- file.path(output, paste0(mode, ".json"))
    if (file.exists(ready)) unlink(ready)
    worker <- processx::process$new(file.path(R.home("bin"), if (.Platform$OS.type == "windows") "Rscript.exe" else "Rscript"),
      c("--vanilla", "scripts/test_yytree_lifecycle.R", paste0("--worker-", mode), egps, ready, if (gui) "--gui"),
      stdout = file.path(logs, paste0("yytree-worker-", mode, ".out")), stderr = file.path(logs, paste0("yytree-worker-", mode, ".err")),
      windows_hide_window = TRUE)
    tryCatch({
      deadline <- Sys.time() + 30
      while (!file.exists(ready) && worker$is_alive() && Sys.time() < deadline) Sys.sleep(.05)
      stopifnot(file.exists(ready))
      java <- jsonlite::fromJSON(ready)$java
      if (mode == "crash") worker$kill() else worker$wait(10000)
      stopifnot(!worker$is_alive())
      deadline <- Sys.time() + 10
      while (running(java) && Sys.time() < deadline) Sys.sleep(.05)
      stopifnot(!running(java))
    }, finally = worker$kill())
  }
  dir.create("logs", showWarnings = FALSE)
  jsonlite::write_json(list(passed = TRUE, gui = gui, large_request_bytes = nchar(payload, type = "bytes")),
    "logs/yytree-lifecycle.json", auto_unbox = TRUE, pretty = TRUE)
  cat("Shared JVM, dispose/reopen, quit/config, restart, crash recovery, UTF-8, large messages and R exit cleanup passed.\n")
}
main()
