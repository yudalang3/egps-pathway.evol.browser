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

test_that("editor-generated R snippets replay 9 shapes, 3 modes, snap, Z-order, empty list, and heatmap edge cases", {
  fixture_path <- test_path("fixtures", "free-shape-roundtrip.json")
  expect_true(file.exists(fixture_path))
  fixtures <- jsonlite::fromJSON(fixture_path, simplifyVector = FALSE)
  expect_gte(length(fixtures), 6L)

  base_plot <- yytree("(A:1,B:2);", width = 1000, height = 800)
  base_snapshot <- serialize(base_plot, NULL)

  to_logical_x <- function(layer, val) switch(layer$mode, relative = val * 1000, percent = val * 10, pixel = val)
  to_logical_y <- function(layer, val) switch(layer$mode, relative = val * 800, percent = val * 8, pixel = val)

  for (fx in fixtures) {
    code <- trimws(fx$code)
    expr <- if (identical(code, "# No free shapes")) "base_plot" else paste("base_plot", code)
    replayed <- eval(parse(text = expr))
    expect_s3_class(replayed, "yytree")
    expect_identical(serialize(base_plot, NULL), base_snapshot)

    actual <- jsonlite::fromJSON(.scene_json(replayed), simplifyVector = FALSE)$layers
    expected <- fx$layers
    expect_length(actual, length(expected))

    for (i in seq_along(actual)) {
      exp_i <- expected[[i]]
      act_i <- actual[[i]]
      expect_identical(act_i$type, exp_i$type)
      for (key in names(exp_i)) {
        if (key %in% c("source", "line_color", "fill_color") && is.null(exp_i[[key]])) next
        if (is.null(exp_i[[key]]) && !key %in% names(act_i)) next
        if (key == "cells") {
          expect_identical(act_i[[key]], exp_i[[key]])
        } else if (key == "source") {
          for (field in c("matrix", "colors", "limits")) {
            expect_equal(act_i$source[[field]], exp_i$source[[field]], tolerance = 1e-12, ignore_attr = TRUE)
          }
        } else {
          expect_equal(act_i[[key]], exp_i[[key]], tolerance = 1e-12, ignore_attr = TRUE)
        }
      }
    }

    checks <- fx$checks
    if (!is.null(checks$modes_expected)) {
      expect_true(all(unlist(checks$modes_expected) %in% vapply(actual, function(l) l$mode, "")))
    }
    if (!is.null(checks$shape_types_expected)) {
      expect_true(all(unlist(checks$shape_types_expected) %in% vapply(actual, function(l) l$type, "")))
    }
    if (!is.null(checks$snap_check)) {
      sc <- checks$snap_check
      layer <- actual[[as.integer(sc$layer_index)]]
      expect_lt(abs(to_logical_x(layer, layer[[sc$x_field]]) - sc$expected_x), 1e-9)
      expect_lt(abs(to_logical_y(layer, layer[[sc$y_field]]) - sc$expected_y), 1e-9)
    }
    if (isTRUE(checks$empty_list)) {
      expect_length(actual, 0L)
      expect_identical(code, "# No free shapes")
    }
  }
})
