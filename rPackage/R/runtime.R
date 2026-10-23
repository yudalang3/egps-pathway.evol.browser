.runtime <- new.env(parent = emptyenv())
config_yytree <- function(path2egps, max_memory = "4g") {
  if (!is.character(path2egps) || length(path2egps) != 1 || is.na(path2egps)) stop("path2egps must be one installation directory.")
  root <- normalizePath(path2egps, winslash = "/", mustWork = TRUE)
  if (!is.character(max_memory) || length(max_memory) != 1 || is.na(max_memory) || !grepl("^[1-9][0-9]*[mMgG]$", max_memory)) stop("max_memory must be like '512m' or '4g'.")
  max_memory <- tolower(max_memory)
  jars <- list.files(file.path(root, "dependency-egps"), "\\.jar$", full.names = TRUE)
  if (!length(jars)) stop("No dependency-egps JARs found at the supplied installation.")
  java <- file.path(root, "jre")
  if (!dir.exists(java)) java <- Sys.getenv("JAVA_HOME")
  if (!nzchar(java)) stop("No bundled Java or JAVA_HOME found.")
  java <- normalizePath(java, winslash = "/", mustWork = TRUE)
  if (exists("config", .runtime, inherits = FALSE)) {
    if (!identical(.runtime$config$root, root) || !identical(.runtime$config$max_memory, max_memory)) stop("JVM configuration is fixed; restart R to change it.")
    return(invisible(.runtime$config))
  }
  if (!"rJava" %in% loadedNamespaces()) Sys.setenv(JAVA_HOME = java)
  if (!requireNamespace("rJava", quietly = TRUE)) stop("Install rJava before configuring yytree.")
  running <- rJava::.jvmState()$initialized
  args <- c(paste0("-Xmx", max_memory), "-Dfile.encoding=UTF-8")
  argfile <- file.path(root, "eGPS2.args")
  if (file.exists(argfile)) {
    extra <- trimws(readLines(argfile, warn = FALSE))
    extra <- extra[nzchar(extra) & !startsWith(extra, "#")]
    extra <- extra[!grepl("^-Xmx", extra)]
    args <- c(args, sub("^(--add-(opens|exports)) ", "\\1=", extra))
  }
  if (running) {
    actual <- rJava::.jcall("java/lang/System", "S", "getProperty", "java.home")
    heap <- rJava::.jcall(rJava::.jcall("java/lang/Runtime", "Ljava/lang/Runtime;", "getRuntime"), "J", "maxMemory")
    requested <- as.numeric(sub("[mMgG]$", "", max_memory)) * if (grepl("[gG]$", max_memory)) 1024^3 else 1024^2
    if (normalizePath(actual, winslash = "/") != java || abs(heap-requested) > requested * .05) stop("Existing JVM conflicts with requested Java or memory; restart R.")
  }
  bridgejar <- system.file("java", "yytree-bridge.jar", package = "yytree")
  if (!nzchar(bridgejar)) stop("The yytree bridge JAR is missing; build and reinstall the package.")
  loaded <- tryCatch({
    if (!running) rJava::.jinit(parameters = args)
    version <- rJava::.jcall("java/lang/System", "S", "getProperty", "java.specification.version")
    if (as.numeric(version) < 25) stop("Java 25+ required; running Java ", version, ". Restart R with a compatible runtime.")
    actual_java <- rJava::.jcall("java/lang/System", "S", "getProperty", "java.home")
    if (normalizePath(actual_java, winslash = "/") != java) stop("Java was already loaded from ", actual_java, "; restart R before selecting ", java)
    urls <- lapply(c(bridgejar, jars), function(path) {
      file <- rJava::.jnew("java/io/File", path)
      uri <- rJava::.jcall(file, "Ljava/net/URI;", "toURI")
      rJava::.jcall(uri, "Ljava/net/URL;", "toURL")
    })
    parent <- rJava::.jcall("java/lang/ClassLoader", "Ljava/lang/ClassLoader;", "getPlatformClassLoader")
    loader <- rJava::.jnew("java/net/URLClassLoader", rJava::.jarray(urls, contents.class = "java/net/URL"), parent)
    bridge <- rJava::.jnew("api/rpython/yytree/YyTreeBridge", class.loader = loader)
    list(loader = loader, bridge = bridge, probe = rJava::.jcall(bridge, "S", "probeRuntime"))
  }, error = function(e) stop("Cannot initialize yytree against ", root, ": ", conditionMessage(e), call. = FALSE))
  bridge <- loaded$bridge
  probe <- loaded$probe
  .runtime$bridge <- bridge
  .runtime$loader <- loaded$loader
  .runtime$config <- list(root = root, java = java, max_memory = max_memory, probe = probe)
  invisible(.runtime$config)
}
print.yytree <- function(x, ..., new_window = FALSE) {
  if (!exists("bridge", .runtime, inherits = FALSE)) stop("Call config_yytree(path2egps) first.")
  if (!is.logical(new_window) || length(new_window) != 1 || is.na(new_window)) stop("new_window must be TRUE or FALSE.")
  payload <- .scene_json(x)
  rJava::.jcall(.runtime$bridge, "S", "showPlot", payload, as.logical(new_window))
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
