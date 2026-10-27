test_that("annotation gaps validate without starting Java", {
  expect_equal(leaf_annotation_gap()$width, 12)
  for (width in c(0, 2.5, 20)) expect_equal(leaf_annotation_gap(width)$width, width)
  for (width in list(NULL, numeric(), c(1, 2), NA_real_, NaN, Inf, -1, "12", TRUE, 1+1i)) {
    error <- tryCatch(leaf_annotation_gap(width), error = conditionMessage)
    for (term in c("leaf_annotation_gap", "width", "Received:", "Expected:", "Example:"))
      expect_match(error, term, fixed = TRUE)
  }
  expect_false(yytree:::.runtime_alive())
})

test_that("gaps preserve append order and scalar wire values", {
  p <- yytree("(A:1,B:1);")
  categories <- data.frame(name = c("A", "B"), group = c("one", "two"))
  values <- matrix(c(1, 2), dimnames = list(c("A", "B"), "score"))
  q <- p + leaf_annotation_gap(99) + leaf_annotation_rect(categories) +
    leaf_annotation_gap(0) + leaf_labels() + leaf_annotation_gap(2.5) +
    leaf_annotation_heatmap(values) + leaf_annotation_gap(20)
  expect_length(p$layers, 0)
  expect_identical(q$tree, p$tree)
  wire <- jsonlite::fromJSON(yytree:::.scene_json(q), simplifyVector = FALSE)
  expect_identical(vapply(wire$layers, `[[`, "", "type"),
    c("annotation_gap", "track", "annotation_gap", "leaf_labels", "annotation_gap", "heatmap", "annotation_gap"))
  expect_equal(wire$layers[[5]]$width, 2.5)
  expect_type(wire$layers[[5]]$width, "double")
  expect_identical(unserialize(serialize(q, NULL)), q)
  expect_false(yytree:::.runtime_alive())
})
