test_that("annotation rotation validates and defaults preserve prior values", {
  categories <- data.frame(name = c("A", "B"), group = c("x", "y"), habitat = c("land", "water"))
  values <- matrix(c(1, 2), dimnames = list(c("A", "B"), "score"))
  expect_equal(leaf_annotation_rect(categories)$text_rotation, 60)
  expect_equal(leaf_annotation_heatmap(values)$text_rotation, 60)
  for (angle in c(-90, -45.5, 0, 45, 90)) {
    expect_equal(leaf_annotation_rect(categories, text_rotation = angle)$text_rotation, angle)
    expect_equal(leaf_annotation_heatmap(values, text_rotation = angle)$text_rotation, angle)
  }
  for (angle in list(NULL, numeric(), c(0, 1), NA_real_, Inf, -91, 91, "45", TRUE, 1+1i)) {
    expect_error(leaf_annotation_rect(categories, text_rotation = angle), "text_rotation")
    expect_error(leaf_annotation_heatmap(values, text_rotation = angle), "text_rotation")
  }
  expect_false(yytree:::.runtime_alive())
})

test_that("custom titles change legend headings without changing input or track names", {
  categories <- data.frame(name = c("A", "B"), group = c("x", "y"), habitat = c("land", "water"))
  values <- matrix(c(1, 2), dimnames = list(c("A", "B"), "score"))
  original <- categories
  plot <- yytree("(A:1,B:1);") +
    leaf_annotation_rect(categories, title = c(group = "Species"), text_rotation = -45) +
    leaf_annotation_heatmap(values, title = "Expression", text_rotation = 90)
  wire <- jsonlite::fromJSON(yytree:::.scene_json(plot), simplifyVector = FALSE)
  expect_identical(wire$layers[[1]]$legend_titles$group, "Species")
  expect_null(wire$layers[[1]]$legend_titles$habitat)
  expect_identical(names(wire$layers[[1]]$columns), c("group", "habitat"))
  expect_equal(wire$layers[[1]]$text_rotation, -45)
  expect_identical(wire$layers[[2]]$legend_title, "Expression")
  expect_identical(wire$layers[[2]]$titles[[1]], "score")
  expect_identical(categories, original)
  expect_identical(leaf_annotation_rect(categories[c("name", "group")], title = "Species")$legend_titles,
                   list(group = "Species"))
  expect_identical(leaf_annotation_rect(categories, title = c(habitat = "Habitat", group = "Species"))$legend_titles,
                   list(habitat = "Habitat", group = "Species"))
  for (title in list("Species", c("Species", "Habitat"), c(unknown = "x"),
                     c(group = "x", group = "y"), c(group = NA_character_), 1, character()))
    expect_error(leaf_annotation_rect(categories, title = title), "title")
  for (title in list(c("x", "y"), NA_character_, 1, character()))
    expect_error(leaf_annotation_heatmap(values, title = title), "title")
  expect_false(yytree:::.runtime_alive())
})
