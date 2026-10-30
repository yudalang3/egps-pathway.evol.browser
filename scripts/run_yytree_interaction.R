# Start from run_yytree_interaction.ps1, which sets the working directory and locale.
args <- commandArgs(trailingOnly = TRUE)
smoke <- "--smoke" %in% args
Sys.setenv(YYTREE_EXAMPLES_NO_GUI = if (smoke) "1" else "0")
dir.create("logs", showWarnings = FALSE)
source("rPackage/human_test/run7_interaction_for_human.R", encoding = "UTF-8")
stopifnot(length(plots) == 6L, identical(p, p_restored))
invisible(lapply(plots, yytree:::.scene_json))
expect_error <- function(expr, pattern) {
  result <- tryCatch({ force(expr); NULL }, error = conditionMessage)
  stopifnot(is.character(result), grepl(pattern, result))
}
expect_error(base_plot + interact(data.frame(name = "Unknown")), "Unknown")
expect_error(interact(data.frame(name = c("Human", "Human"))), "unique")
expect_error(yytree("((A:1,B:1)A:1,C:2)Root;") + interact(data.frame(name = "A")), "Ambiguous")
expect_error(interact(data.frame(name = "Human", value = I(list(list(1))))), "Unsupported")
writeLines(yytree:::.scene_json(p), "temp/yytree-human-interaction/scene.json", useBytes = TRUE)
writeLines(c(paste("Started:", Sys.time()), paste("R PID:", Sys.getpid()),
             paste("Smoke:", smoke), "Six example objects, serialization and four invalid-input checks passed."),
           "logs/yytree-interaction-launch.txt")
if (!smoke) {
  stop_file <- "temp/yytree-human-interaction/stop"
  if (file.exists(stop_file)) unlink(stop_file)
  message("Preview ready. Create ", stop_file, " to exit; timeout: 30 minutes.")
  deadline <- Sys.time() + 1800
  while (Sys.time() < deadline && !file.exists(stop_file)) Sys.sleep(1)
  quit_yytree()
}
