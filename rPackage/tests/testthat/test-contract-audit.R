test_that("tree conversion preserves merge heights, order and caller data", {
  clustered <- hclust(dist(USArrests[1:8, ]))
  original <- clustered
  tree <- yytree(clustered)$tree
  n <- length(tree$labels)
  for (i in seq_len(nrow(clustered$merge))) {
    rows <- tree$edges[tree$edges[, 1] == n + i, , drop = FALSE]
    children <- clustered$merge[i, ]
    heights <- vapply(children, function(k) if (k < 0) 0 else clustered$height[k], 0.0)
    expect_equal(rows[, 3] + heights, rep(clustered$height[i], 2))
  }
  walk <- function(id) {
    children <- tree$edges[tree$edges[, 1] == id, 2]
    if (!length(children)) return(id)
    unlist(lapply(children, walk), use.names = FALSE)
  }
  expect_equal(walk(tree$root), clustered$order)
  expect_identical(clustered, original)
  clustered$labels[] <- ""
  expect_identical(yytree(clustered)$tree$labels, as.character(seq_len(n)))
  phy <- ape::read.tree(text = "((B:0,A:0.12345678912345):2,C:3);")
  original <- phy
  expect_equal(unname(yytree(phy)$tree$edges), unname(cbind(phy$edge, phy$edge.length)))
  expect_identical(phy, original)
  phy$edge[2, 2] <- phy$edge[1, 2]
  expect_error(yytree(phy), "multiple parents")
})

test_that("layers append immutably and validate selectors independently", {
  x <- yytree("((A:1,B:2):1,C:3);")
  y <- x + leaf_labels() + leaf_render(size = 3, fill = "blue") +
    leaf_render(df = data.frame(name = "B", fill = NA_character_)) +
    inner_node_annotation("A", "B", "first") +
    inner_node_annotation("A", "C", "second")
  expect_length(x$layers, 0)
  expect_length(y$layers, 5)
  expect_identical(y$layers[[4]]$name2, "B")
  expect_identical(y$layers[[5]]$name2, "C")
  expect_error(y + inner_node_annotation(text = "missing selector"), "two distinct")
  expect_error(y + leaf_labels(FALSE) + leaf_label_render(data.frame(name = "A", color = "red")), "leaf_labels")
  expect_error(x + inner_node_render(df = data.frame(name1 = "A", name2 = "Z")), "Unknown")
  expect_error(x + leaf_render(df = data.frame(name = c("A", "A"), size = 2)), "Duplicate")
})

test_that("annotation mapping, append order and missing cells survive serialization", {
  x <- yytree("((A:1,B:2):1,C:3);")
  df <- data.frame(name = c("C", "A"), group = c("z", NA_character_))
  m <- matrix(c(0, NA, 10, 5), 2, dimnames = list(c("B", "A"), c("one", "two")))
  original <- m
  y <- x + leaf_annotation_rect(df, colors = c(z = "#12345678"), legend = "none") +
    leaf_annotation_heatmap(m, limits = c(0, 20), legend = "bottom_left")
  wire <- jsonlite::fromJSON(yytree:::.scene_json(y), simplifyVector = FALSE)
  expect_identical(unlist(wire$layers[[1]]$names), c("C", "A"))
  expect_identical(wire$layers[[1]]$columns$group$colors[[1]], "#12345678")
  expect_null(wire$layers[[1]]$columns$group$values[[2]])
  expect_identical(unlist(wire$layers[[2]]$names), c("B", "A"))
  expect_equal(unlist(wire$layers[[2]]$limits), c(0, 20))
  expect_null(wire$layers[[2]]$cells[[2]][[1]])
  expect_identical(m, original)
  for (position in c("top_left", "top_right", "bottom_left", "bottom_right", "none"))
    expect_identical(leaf_annotation_heatmap(m, legend = position)$legend, position)
  expect_error(x + leaf_annotation_rect(data.frame(name = "Z", group = "x")), "known")
  rownames(m) <- c("A", "A")
  expect_error(x + leaf_annotation_heatmap(m), "unique")
})

test_that("heatmaps share the raw scale and support clipping and all missing data", {
  m <- matrix(c(-10, 0, 10, NA_real_), 2, dimnames = list(c("A", "B"), c("one", "two")))
  mapped <- leaf_annotation_heatmap(m, colors = c("black", "white"), limits = c(0, 10))
  expect_identical(mapped$cells[1, 1], mapped$cells[2, 1])
  expect_identical(mapped$cells[1, 2], "#FFFFFFFF")
  expect_true(is.na(mapped$cells[2, 2]))
  expect_equal(leaf_annotation_heatmap(m)$limits, c(-10, 10))
  m[] <- NA_real_
  expect_true(all(is.na(leaf_annotation_heatmap(m)$cells)))
  expect_equal(leaf_annotation_heatmap(m)$limits, c(0, 1))
})

test_that("heatmap normalization handles extreme finite values without missing cells", {
  m <- matrix(c(-1e308, 0, 1e308, NA_real_), 4,
              dimnames = list(c("A", "B", "C", "D"), "value"))
  mapped <- leaf_annotation_heatmap(m, colors = c("black", "white"))
  expect_identical(mapped$cells[1:3, 1], c("#000000FF", "#808080FF", "#FFFFFFFF"))
  expect_true(is.na(mapped$cells[4, 1]))
  expect_identical(free_heatmap(.1, .1, .2, .2, m, colors = c("black", "white"))$cells, mapped$cells)
  expect_identical(leaf_annotation_heatmap(m, colors = c("black", "white"), limits = c(-1, 1))$cells, mapped$cells)
  expect_equal(mapped$limits, c(-1e308, 1e308))
})

test_that("deep and multifurcating trees preserve data and disconnected cycles fail", {
  for (type in c("left", "star")) {
    tree <- ape::stree(10000, type = type)
    original <- tree
    value <- yytree(tree)
    expect_equal(unname(value$tree$edges[, 1:2]), unname(tree$edge))
    expect_identical(value$tree$labels, tree$tip.label)
    expect_identical(tree, original)
  }
  invalid <- list(labels = c("A", "B"), root = 3,
    edges = rbind(c(3, 1, 1), c(3, 2, 1), c(4, 5, 1), c(5, 4, 1)))
  expect_error(yytree:::.validate_tree(invalid), "disconnected")
})
