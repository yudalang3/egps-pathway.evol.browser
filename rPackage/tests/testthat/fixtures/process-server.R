mode <- commandArgs(trailingOnly = TRUE)[1]
input <- file("stdin", "r")
request <- jsonlite::fromJSON(readLines(input, n = 1, warn = FALSE))
if (mode == "exit") quit(status = 7)
if (mode == "timeout") Sys.sleep(60)
if (mode == "mismatch") request$id <- request$id + 1
result <- if (mode == "large") paste(rep("Unicode \u6811 \u53f6 ", 30000), collapse = "") else "ok"
response <- paste0(jsonlite::toJSON(list(id = request$id, ok = TRUE, result = result), auto_unbox = TRUE), "\n")
# Force an incomplete line before the rest of the response arrives.
cat(substr(response, 1, 10)); flush.console()
Sys.sleep(.05)
cat(substr(response, 11, nchar(response))); flush.console()
Sys.sleep(60)
