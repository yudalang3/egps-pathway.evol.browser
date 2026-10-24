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
  expect_error(config_yytree(".", max_memory = "0g"), "max_memory must")
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
