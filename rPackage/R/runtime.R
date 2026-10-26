.runtime <- new.env(parent = emptyenv())
.runtime$next_id <- 0

.runtime_alive <- function() {
  exists("process", .runtime, inherits = FALSE) && .runtime$process$is_alive()
}

.notify_parameter_error <- function(message) {
  if (interactive() && Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1" && .runtime_alive())
    tryCatch(.runtime_exchange(.runtime$process, "showParameterError", list(message)),
             error = function(e) invisible(NULL))
  invisible(NULL)
}

.runtime_exchange <- function(process, command, arguments = list(), timeout = 30, runtime = .runtime) {
  if (!is.numeric(timeout) || length(timeout) != 1 || !is.finite(timeout) || timeout <= 0) stop("timeout must be positive and finite.")
  runtime$next_id <- runtime$next_id + 1
  id <- runtime$next_id
  request <- jsonlite::toJSON(list(id = id, command = command, args = unname(arguments)), auto_unbox = TRUE, null = "null", digits = NA)
  pending <- charToRaw(enc2utf8(paste0(request, "\n")))
  started <- proc.time()[["elapsed"]]
  response <- tryCatch({
    output <- ""
    repeat {
      if (proc.time()[["elapsed"]] - started >= timeout) stop("Java request timed out.")
      if (length(pending)) pending <- process$write_input(pending)
      process$poll_io(10)
      output <- paste0(output, process$read_output())
      if (grepl("\n", output, fixed = TRUE)) {
        if (length(pending)) stop("Java replied before the request was fully written.")
        reply <- jsonlite::fromJSON(output, simplifyVector = FALSE)
        if (!is.list(reply) || !isTRUE(reply$id == id) || !is.logical(reply$ok) || length(reply$ok) != 1 || is.na(reply$ok)) stop("Invalid Java protocol response.")
        break
      }
      if (!process$is_alive()) stop("Java process exited before replying.")
    }
    reply
  }, error = function(e) {
    try(process$kill(), silent = TRUE)
    logfile <- process$get_error_file()
    detail <- if (!is.null(logfile) && file.exists(logfile)) paste(utils::tail(readLines(logfile, warn = FALSE, encoding = "UTF-8"), 12), collapse = "\n") else ""
    stop(conditionMessage(e), " No request was retried.",
         if (nzchar(detail)) paste0("\n", detail), if (!is.null(logfile)) paste0("\nJava log: ", logfile), call. = FALSE)
  }, interrupt = function(e) {
    try(process$kill(), silent = TRUE)
    stop(e)
  })
  if (!isTRUE(response$ok)) {
    message <- paste0("Java ", command, " failed: ", response$error)
    if (command == "showPlot") .notify_parameter_error(message)
    stop(message, call. = FALSE)
  }
  response$result
}

.runtime_stop <- function(runtime = .runtime) {
  if (!exists("process", runtime, inherits = FALSE)) return(invisible(NULL))
  process <- runtime$process
  on.exit({
    try(if (process$is_alive()) process$kill(), silent = TRUE)
    if (exists("process", runtime, inherits = FALSE) && identical(runtime$process, process))
      rm("process", envir = runtime)
  }, add = TRUE)
  if (process$is_alive()) {
    deadline <- proc.time()[["elapsed"]] + 2
    # One bounded shutdown exchange includes disposing all windows.
    tryCatch(.runtime_exchange(process, "shutdown", timeout = 2, runtime = runtime), error = function(e) NULL)
    if (process$is_alive()) process$wait(max(0, (deadline - proc.time()[["elapsed"]]) * 1000))
  }
  invisible(NULL)
}

quit_yytree <- function() {
  runtime <- .runtime
  on.exit({
    fields <- intersect(c("config", "launch"), ls(runtime))
    if (length(fields)) rm(list = fields, envir = runtime)
  }, add = TRUE)
  .runtime_stop(runtime)
  invisible(NULL)
}

.runtime_finalize <- function(runtime) {
  tryCatch(.runtime_stop(runtime), error = function(e) NULL, interrupt = function(e) NULL)
}

reg.finalizer(.runtime, .runtime_finalize, onexit = TRUE)

.onUnload <- function(libpath) quit_yytree()

config_yytree <- function(path2egps, max_memory_heap = "4g", max_memory_stack = "8m", restart = FALSE) {
  if (!is.character(path2egps) || length(path2egps) != 1 || is.na(path2egps)) stop("path2egps must be one installation directory.")
  if (!is.logical(restart) || length(restart) != 1 || is.na(restart)) stop("restart must be TRUE or FALSE.")
  root <- normalizePath(path2egps, winslash = "/", mustWork = TRUE)
  if (!is.character(max_memory_heap) || length(max_memory_heap) != 1 || is.na(max_memory_heap) || !grepl("^[1-9][0-9]*[mMgG]$", max_memory_heap)) stop("max_memory_heap must be like '512m' or '4g'.")
  if (!is.character(max_memory_stack) || length(max_memory_stack) != 1 || is.na(max_memory_stack) || !grepl("^[1-9][0-9]*[kKmMgG]$", max_memory_stack)) stop("max_memory_stack must be like '512k' or '8m'.")
  max_memory_heap <- tolower(max_memory_heap)
  max_memory_stack <- tolower(max_memory_stack)
  jars <- list.files(file.path(root, "dependency-egps"), "\\.jar$", full.names = TRUE)
  if (!length(jars)) stop("No dependency-egps JARs found at the supplied installation.")
  java <- file.path(root, "jre")
  if (!dir.exists(java)) java <- Sys.getenv("JAVA_HOME")
  if (!nzchar(java)) stop("No bundled Java or JAVA_HOME found.")
  java <- normalizePath(java, winslash = "/", mustWork = TRUE)
  executable <- file.path(java, "bin", if (.Platform$OS.type == "windows") "java.exe" else "java")
  if (!file.exists(executable)) stop("Java executable not found: ", executable)
  if (exists("config", .runtime, inherits = FALSE) && !restart) {
    if (!identical(.runtime$config$root, root) || !identical(.runtime$config$java, java) ||
        !identical(.runtime$config$max_memory_heap, max_memory_heap) || !identical(.runtime$config$max_memory_stack, max_memory_stack)) stop("Use restart = TRUE to change Java configuration; existing windows will close.")
    if (.runtime_alive()) return(invisible(.runtime$config))
  }
  bridgejar <- system.file("java", "yytree-bridge.jar", package = "yytree")
  if (!nzchar(bridgejar)) stop("The yytree bridge JAR is missing; build and reinstall the package.")
  args <- c(paste0("-Xmx", max_memory_heap), paste0("-Xss", max_memory_stack), "-Dfile.encoding=UTF-8")
  argfile <- file.path(root, "eGPS2.args")
  if (file.exists(argfile)) {
    extra <- trimws(readLines(argfile, warn = FALSE))
    extra <- extra[nzchar(extra) & !startsWith(extra, "#") & !grepl("^-X(mx|ss)|^-XX:(MaxHeapSize|ThreadStackSize)=", extra)]
    args <- c(args, sub("^(--add-(opens|exports)) ", "\\1=", extra))
  }
  args <- c(args, "-cp", paste(c(bridgejar, jars), collapse = .Platform$path.sep), "api.rpython.yytree.YyTreeProcess")
  .runtime_start(list(root = root, java = java, max_memory_heap = max_memory_heap, max_memory_stack = max_memory_stack),
                 list(command = executable, args = args))
}

.runtime_start <- function(config, launch) {
  .runtime_stop()
  logdir <- file.path(tempdir(), "yytree", "logs")
  dir.create(logdir, recursive = TRUE, showWarnings = FALSE)
  logfile <- tempfile("java-", tmpdir = logdir, fileext = ".log")
  process <- processx::process$new(launch$command, launch$args, stdin = "|", stdout = "|", stderr = logfile,
                                  encoding = "UTF-8", cleanup = TRUE, supervise = TRUE, windows_hide_window = TRUE)
  initialized <- FALSE
  on.exit(if (!initialized) try(process$kill(), silent = TRUE), add = TRUE)
  probe <- tryCatch(.runtime_exchange(process, "probeRuntime"), error = function(e) {
    stop("Cannot initialize yytree against ", config$root, ". Java 25+ is required. ", conditionMessage(e), call. = FALSE)
  })
  .runtime$process <- process
  .runtime$config <- c(config[c("root", "java", "max_memory_heap", "max_memory_stack")],
                       list(probe = probe, pid = process$get_pid(), log = logfile))
  .runtime$launch <- launch
  initialized <- TRUE
  invisible(.runtime$config)
}

.runtime_request <- function(command, ..., timeout = 30) {
  if (!exists("config", .runtime, inherits = FALSE)) stop("Call config_yytree(path2egps) first.")
  if (!.runtime_alive()) {
    .runtime_start(.runtime$config, .runtime$launch)
    warning("Java restarted; previous windows and their zoom/scroll state were lost.", call. = FALSE)
  }
  .runtime_exchange(.runtime$process, command, list(...), timeout = timeout)
}

print.yytree <- function(x, ..., new_window = FALSE) {
  if (!is.logical(new_window) || length(new_window) != 1 || is.na(new_window)) stop("new_window must be TRUE or FALSE.")
  .runtime_request("showPlot", .scene_json(x), new_window)
  invisible(x)
}

.scene_json <- function(scene) {
  scene$layers <- lapply(scene$layers, function(layer) {
    if (!is.null(layer$fill)) layer$fill <- .color(layer$fill)
    if (!is.null(layer$df)) for (column in intersect(c("fill", "color"), names(layer$df))) layer$df[[column]] <- .color(layer$df[[column]])
    layer
  })
  arrays <- c("labels", "margins", "names", "levels", "colors", "values", "titles", "limits", "palette", "order")
  normalize <- function(value, key = "") {
    if (is.data.frame(value) || is.matrix(value)) return(value)
    if (is.list(value)) return(lapply(seq_along(value), function(i) normalize(value[[i]], if (is.null(names(value))) "" else names(value)[i])) |> stats::setNames(names(value)))
    if (key %in% arrays) I(value) else value
  }
  jsonlite::toJSON(normalize(unclass(scene)), auto_unbox = TRUE, dataframe = "rows", matrix = "rowmajor", digits = NA, na = "null", null = "null")
}
