test_that("clade text positions validate without starting Java", {
  expect_identical(inner_node_annotation("A", "B", "AB")$text_position, "right")
  expect_identical(inner2leaf_highlight("A", "B")$text_position, "left")
  expect_null(inner2leaf_highlight("A", "B")$text)
  for (position in c("left", "right", "top", "bottom", "topleft", "topright", "bottomleft", "bottomright")) {
    expect_identical(inner_node_annotation("A", "B", "AB", text_position = position)$text_position, position)
    expect_identical(inner2leaf_highlight("A", "B", text = "AB", text_position = position)$text_position, position)
  }
  for (position in list(NULL, NA_character_, character(), c("left", "top"), "middle", 1)) {
    expect_error(inner_node_annotation("A", "B", "AB", text_position = position), "text_position")
    expect_error(inner2leaf_highlight("A", "B", text_position = position), "text_position")
  }
  for (text in list(NA_character_, c("A", "B"), 1, character())) {
    expect_error(inner_node_annotation("A", "B", text), "text")
    expect_error(inner2leaf_highlight("A", "B", text = text), "text")
  }
  expect_false(yytree:::.runtime_alive())
})

test_that("corner positions survive row overrides and serialization", {
  df <- data.frame(name1 = rep("A", 4), name2 = c("B", "C", "D", "E"), text = letters[1:4],
                   text_position = c("topleft", "topright", "bottomleft", "bottomright"))
  p <- yytree("((A:1,B:1):1,(C:1,(D:1,E:1):1):1);")
  for (layer in list(inner_node_annotation(df = df), inner2leaf_highlight(df = df))) {
    wire <- jsonlite::fromJSON(yytree:::.scene_json(p + layer), simplifyVector = FALSE)
    expect_identical(vapply(wire$layers[[1]]$df, `[[`, "", "text_position"), df$text_position)
  }
  expect_error(inner_node_annotation("A", "B", "AB", text_position = "top_left"), "text_position")
  expect_error(inner2leaf_highlight("A", "B", text_position = "BOTTOMRIGHT"), "text_position")
})

test_that("clade rows and original positional arguments retain their meaning", {
  p <- yytree("((A:1,B:1):1,C:2);")
  df <- data.frame(name1 = c("A", "A"), name2 = c("B", "C"),
                   text = c("哺乳动物", ""), text_position = c("top", "bottom"))
  original <- df
  q <- p + inner_node_annotation(df = df) + inner2leaf_highlight(fill = NA, df = df)
  wire <- jsonlite::fromJSON(yytree:::.scene_json(q), simplifyVector = FALSE)
  expect_identical(wire$layers[[1]]$df[[1]]$text, "哺乳动物")
  expect_identical(wire$layers[[1]]$df[[2]]$text_position, "bottom")
  expect_null(wire$layers[[2]]$fill)
  expect_identical(df, original)
  expect_length(p$layers, 0)
  expect_identical(inner_node_annotation("A", "B", "AB", "ellipse", 30, "gold", NULL)$shape, "ellipse")
  expect_identical(inner2leaf_highlight("A", "B", "red", NULL)$fill, "red")
  q <- p + inner2leaf_highlight("A", "B", text = "Fallback", df = df[c("name1", "name2")])
  expect_identical(q$layers[[1]]$text, "Fallback")
  df$text_position[1] <- "invalid"
  expect_error(inner2leaf_highlight(df = df), "df\\$text_position")
  df <- original
  df$text[1] <- NA_character_
  expect_error(inner_node_annotation(df = df), "df\\$text")
  expect_false(yytree:::.runtime_alive())
})
