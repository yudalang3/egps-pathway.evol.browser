# Validate the public memory parameters, deep topology and recoverable stack errors.
args <- commandArgs(trailingOnly = TRUE)
.libPaths(c(normalizePath("temp/yytree-library", winslash = "/"), .libPaths()))
library(yytree)

main <- function() {
  egps <- args[1]
  gui <- "--gui" %in% args
  on.exit(quit_yytree(), add = TRUE)
  output <- file.path(getwd(), "temp", "yytree-deep-trees")
  dir.create(output, recursive = TRUE, showWarnings = FALSE)
  configuration <- config_yytree(egps)
  pid <- configuration$pid
  stopifnot(configuration$max_memory_heap == "4g", configuration$max_memory_stack == "8m",
            all(c("-Xmx4g", "-Xss8m") %in% jsonlite::fromJSON(configuration$probe)$jvmArguments))
  results <- list()
  for (n in c(10000L, 20000L)) {
    tree <- ape::stree(n, type = "left")
    construction <- system.time(base <- yytree(tree, width = 1200, height = 900))[["elapsed"]]
    elapsed <- numeric(7)
    for (code in 1:7) {
      plot <- base + yytree::layout(code = code)
      path <- file.path(output, paste0(n, "-layout-", code, ".png"))
      elapsed[code] <- system.time(metadata <- jsonlite::fromJSON(
        yytree:::.runtime_request("renderPng", yytree:::.scene_json(plot), path)))[["elapsed"]]
      stopifnot(metadata$leaves == n, metadata$layout == code, file.info(path)$size > 0,
                yytree:::.runtime$process$get_pid() == pid)
      tips <- unlist(metadata$tips, use.names = FALSE)
      stopifnot(all(is.finite(tips)))
      if (gui) {
        stopifnot(identical(print(plot), plot))
        yytree:::.runtime_request("closeAll")
        stopifnot(yytree:::.runtime$process$get_pid() == pid)
      }
    }
    results[[as.character(n)]] <- list(construction_seconds = construction, render_seconds = elapsed)
    cat(n, "deep leaves: seven layouts passed; R construction", construction, "seconds.\n")
  }

  # A small stack must expose the real cause without destroying the session.
  config_yytree(egps, max_memory_stack = "256k", restart = TRUE)
  process <- yytree:::.runtime$process
  payload <- yytree:::.scene_json(yytree(ape::stree(10000, type = "left")))
  failure <- tryCatch(yytree:::.runtime_request("renderPng", payload, file.path(output, "low-stack.png")),
                      error = conditionMessage)
  stopifnot(grepl("StackOverflowError", failure, fixed = TRUE),
            grepl("max_memory_stack", failure, fixed = TRUE),
            !grepl("InvocationTargetException", failure, fixed = TRUE), process$is_alive())
  simple <- yytree("(A:1,B:2);")
  small <- jsonlite::fromJSON(yytree:::.runtime_request("renderPng",
    yytree:::.scene_json(simple), file.path(output, "after-stack-error.png")))
  stopifnot(small$leaves == 2, process$get_pid() == yytree:::.runtime$process$get_pid())

  # On Windows, JAVA_TOOL_OPTIONS may enlarge EDT stacks but leave main smaller.
  # Exercise an error in the command thread as well as the wrapped EDT error above.
  if (.Platform$OS.type == "windows") {
    previous <- Sys.getenv("JAVA_TOOL_OPTIONS", unset = NA_character_)
    on.exit(if (is.na(previous)) Sys.unsetenv("JAVA_TOOL_OPTIONS") else Sys.setenv(JAVA_TOOL_OPTIONS = previous), add = TRUE)
    Sys.setenv(JAVA_TOOL_OPTIONS = "-Xss8m")
    launch <- yytree:::.runtime$launch
    launch$args <- launch$args[!startsWith(launch$args, "-Xss")]
    yytree:::.runtime_start(yytree:::.runtime$config, launch)
    process <- yytree:::.runtime$process
    failure <- tryCatch(yytree:::.runtime_request("renderPng", payload, file.path(output, "main-stack.png")),
                        error = conditionMessage)
    stopifnot(grepl("StackOverflowError", failure, fixed = TRUE), process$is_alive())
    small <- jsonlite::fromJSON(yytree:::.runtime_request("renderPng",
      yytree:::.scene_json(simple), file.path(output, "after-main-stack-error.png")))
    stopifnot(small$leaves == 2, process$get_pid() == yytree:::.runtime$process$get_pid())
    if (is.na(previous)) Sys.unsetenv("JAVA_TOOL_OPTIONS") else Sys.setenv(JAVA_TOOL_OPTIONS = previous)
  }
  config_yytree(egps, max_memory_stack = "16m", restart = TRUE)
  stopifnot("-Xss16m" %in% jsonlite::fromJSON(yytree:::.runtime$config$probe)$jvmArguments)
  if (gui) print(simple)
  dir.create("logs", showWarnings = FALSE)
  jsonlite::write_json(list(passed = TRUE, gui = gui, stack = "8m", results = results),
    "logs/yytree-deep-trees.json", auto_unbox = TRUE, pretty = TRUE)
  cat("Heap/stack arguments, deep trees and recovery after EDT/main stack overflow passed.\n")
}
main()
