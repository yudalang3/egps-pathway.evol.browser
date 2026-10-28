test_that("export validates inputs before requesting Java", {
  expect_error(export(NULL, "a.pdf"), "yytree")
  p <- yytree("(A:1,B:2);")
  expect_error(export(p, NA_character_), "path")
  expect_error(export(p, "a.png"), "extension")
  quit_yytree()
  expect_error(export(p, tempfile(fileext = ".PDF")), "config_yytree")
})
