# 26. 保存和恢复绘图定义
# Run in R/RStudio after installing yytree. Set EGPS_HOME for another installation.
library(yytree)
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
leaf_names <- yytree(tree)$tree$labels
categories <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))
set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))

y <- yytree(tree) + leaf_labels() + inner2leaf_highlight("Human", "Mouse")
path <- tempfile(fileext = ".rds")
saveRDS(y, path)
restored <- readRDS(path)
stopifnot(identical(y, restored))
unlink(path)
y <- restored
# RDS stores ordinary R data, not Java handles; configure Java again in a new R session.

if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(y)

