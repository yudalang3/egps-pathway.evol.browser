# Execute the shipped interactive example, capture the exact renderer, then close its windows.
.libPaths(c(normalizePath("temp/yytree-library", winslash = "/"), .libPaths()))
source("scripts/yytree_example.R", encoding = "UTF-8")
result <- yytree:::.runtime_request("renderPng", yytree:::.scene_json(y),
                       file.path(normalizePath("temp/yytree-qa", winslash = "/"), "complete-example.png"))
stopifnot(jsonlite::fromJSON(result)$leaves == 9L)
yytree:::.runtime_request("closeAll")
cat("Complete nine-species example passed.\n")
