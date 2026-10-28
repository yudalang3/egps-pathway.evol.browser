test_that("arrow heads have validated independent dimensions", {
  arrow <- free_arrow(.1, .2, .3, .4, line_thickness = 5, head_style = "filled", head_length = 20, head_width = 14)
  expect_equal(arrow$head_style, "filled")
  expect_equal(arrow$head_length, 20)
  expect_equal(arrow$head_width, 14)
  expect_equal(free_arrow(0, 0, 1, 1)$head_style, "open")
  expect_null(free_arrow(0, 0, 1, 1)$head_length)
  for (value in list(0, -1, Inf, NA_real_, c(1, 2), "10", 1+1i)) {
    expect_error(free_arrow(0, 0, 1, 1, head_length = value), "head_length")
    expect_error(free_arrow(0, 0, 1, 1, head_width = value), "head_width")
  }
  expect_error(free_arrow(0, 0, 1, 1, head_style = "round"))
})

test_that("free heatmap retains reproducible source data", {
  values <- matrix(c(-1, NA, 2, 5), 2)
  layer <- free_heatmap(.1, .2, .3, .4, values, colors = c("#12345680", "white"), limits = c(-1, 5))
  expect_identical(layer$source$matrix, values)
  expect_equal(layer$source$colors, c("#12345680", "#FFFFFFFF"))
  expect_equal(layer$source$limits, c(-1, 5))
  expect_identical(layer$cells, .heat_colors(values, c("#12345680", "white"), c(-1, 5))$cells)
  expect_null(free_heatmap(0, 0, 1, 1, values)$source$limits)
  scene <- yytree("(A:1,B:1);") + layer
  decoded <- jsonlite::fromJSON(.scene_json(scene), simplifyVector = FALSE)$layers[[1]]$source
  expect_length(decoded$matrix, 2)
  expect_length(decoded$colors, 2)
  expect_silent(.scene_json(yytree("(A:1,B:1);") + free_heatmap(0, 0, 1, 1, values)))
})
