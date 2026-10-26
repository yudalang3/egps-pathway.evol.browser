test_that("loading and building values does not start Java", {
  expect_false(yytree:::.runtime_alive())
  tree <- hclust(dist(USArrests))
  x <- yytree(tree)
  y <- x + yytree::layout(name = "slant") + leaf_labels()
  expect_length(x$layers, 0)
  expect_length(y$layers, 2)
  expect_equal(x$tree$order, tree$order)
  expect_equal(nrow(x$tree$edges), 2 * (nrow(tree$merge)))
  expect_equal(unserialize(serialize(y, NULL)), y)
})

test_that("label gap and layout parameters validate without starting Java", {
  expect_equal(leaf_labels()$gap_length, 6)
  expect_equal(leaf_labels(gap_length = 0)$gap_length, 0)
  expect_equal(leaf_labels(gap_length = 2.5)$gap_length, 2.5)
  expect_equal(leaf_labels()$align, "right")
  for (value in list(NULL, numeric(), c(1, 2), NA_real_, NaN, Inf, -1, "6", TRUE, 1+1i)) {
    error <- tryCatch(leaf_labels(gap_length = value), error = conditionMessage)
    expect_type(error, "character")
    for (term in c("leaf_labels", "gap_length", "Received:", "Expected:", "Example:"))
      expect_match(error, term, fixed = TRUE)
  }
  for (value in list(NA_character_, character(), c("left", "right"), "middle", 1))
    expect_error(leaf_labels(align = value), "align")
  expect_error(leaf_labels(gap = 4, typo = TRUE), "Only these named parameters")
  expect_error(yytree::layout(code = c(3, 4)), "1 through 7")
  expect_error(yytree::layout(name = c("circ", "slant")), "name")
  expect_error(yytree::layout(code = 3, angle_typo = 90), "Only these named parameters")
  for (code in c(3, 4)) {
    layer <- yytree::layout(code = code, start_angle = 330, angle_extent = 90)
    expect_equal(layer$start_angle, 330)
    expect_equal(layer$angle_extent, 90)
    expect_error(yytree::layout(code = code, tree_width_ratio = .8), "only to slanted layout 7")
    for (value in list(-1, 361, 1.5, NA, Inf, c(1, 2), "90"))
      expect_error(yytree::layout(code = code, start_angle = value), "start_angle")
    expect_error(yytree::layout(code = code, angle_extent = 0), "angle_extent")
    expect_equal(yytree::layout(code = code, start_angle = 0, angle_extent = 1)$angle_extent, 1)
    expect_equal(yytree::layout(code = code, start_angle = 360, angle_extent = 360)$start_angle, 360)
  }
  expect_equal(yytree::layout(code = 7, tree_width_ratio = .8, left_margin_ratio = 0)$left_margin_ratio, 0)
  expect_error(yytree::layout(code = 7, start_angle = 90), "only to circular layouts 3 and 4")
  for (code in c(1, 2, 5, 6)) {
    expect_error(yytree::layout(code = code, angle_extent = 90), "only to circular")
    expect_error(yytree::layout(code = code, left_margin_ratio = .2), "only to slanted")
  }
  for (value in list(0, -1, 1.1, NA, Inf, numeric(), "1"))
    expect_error(yytree::layout(code = 7, tree_width_ratio = value), "tree_width_ratio")
  for (value in list(-.1, 1, NA, Inf, c(0, .2)))
    expect_error(yytree::layout(code = 7, left_margin_ratio = value), "left_margin_ratio")
  x <- yytree("(A:1,B:2);")
  y <- x + leaf_labels(gap_length = 30) + leaf_labels() +
    yytree::layout(code = 3, start_angle = 0) + yytree::layout(code = 3)
  expect_length(x$layers, 0)
  expect_equal(y$layers[[2]]$gap_length, 6)
  expect_null(y$layers[[4]]$start_angle)
  expect_false(yytree:::.runtime_alive())
})

test_that("parameter dialogs only use an existing interactive runtime", {
  notify <- yytree:::.notify_parameter_error
  state <- new.env(parent = environment(notify))
  environment(notify) <- state
  state$interactive <- function() TRUE
  state$.runtime_alive <- function() TRUE
  state$Sys.getenv <- function(...) ""
  state$.runtime <- list(process = "existing")
  calls <- list()
  state$.runtime_exchange <- function(process, command, arguments) {
    calls[[length(calls) + 1L]] <<- list(process, command, arguments)
  }
  notify("Received: -1\nExpected: nonnegative\nExample: leaf_labels(gap_length = 6)")
  expect_length(calls, 1)
  expect_equal(calls[[1]][1:2], list("existing", "showParameterError"))
  state$interactive <- function() FALSE
  notify("batch error")
  state$interactive <- function() TRUE
  state$Sys.getenv <- function(...) "1"
  notify("GUI disabled")
  state$Sys.getenv <- function(...) ""
  state$.runtime_alive <- function() FALSE
  notify("runtime stopped")
  expect_length(calls, 1)
  state$.runtime_alive <- function() TRUE
  state$.runtime_exchange <- function(...) stop("dialog unavailable")
  expect_silent(notify("original parameter error"))
})
test_that("Newick and phylo preserve input branch lengths", {
  phy <- ape::read.tree(text = "((A:0.12345678912345,B:2):3,C:4);")
  expect_equal(yytree(phy)$tree$edges[,3], phy$edge.length)
  expect_equal(yytree("((A:0.12345678912345,B:2):3,C:4);")$tree, yytree(phy)$tree)
  path <- tempfile(fileext = ".nwk")
  writeLines("(A:1,B:2);", path)
  expect_equal(yytree(path)$tree, yytree("(A:1,B:2);")$tree)
  expect_equal(yytree("(A,B);")$tree$edges[,3], c(1,1))
  unnamed <- phy
  unnamed$tip.label <- NULL
  expect_equal(yytree(unnamed)$tree$labels,as.character(1:3))
  expect_equal(yytree("(:1,:2);")$tree$labels, c("1", "2"))
  expect_equal(yytree("(,);")$tree$labels, c("1", "2"))
  unnamed$tip.label <- rep("", 3)
  expect_equal(yytree(unnamed)$tree$labels, as.character(1:3))
  expect_identical(unnamed$tip.label, rep("", 3))
  expect_error(yytree("(A:1,:2);"), "nonempty")
})
test_that("selectors and label dependency fail early", {
  x <- yytree("((A:1,B:2):1,C:3);")
  expect_error(x + leaf_label_render(data.frame(name = "A", color = "red")), "leaf_labels")
  expect_error(x + leaf_render(df = data.frame(name = "Z", fill = "red")), "Unknown")
  expect_error(x + inner_node_annotation("A", "A", "bad"), "distinct")
  expect_error(x + leaf_annotation_rect(data.frame(name = c("A","A"), group = c("x","y"))), "unique")
  expect_error(yytree("(A:1,A:2);"), "unique")
  expect_error(yytree::layout(code = 8), "1 through 7")
  expect_error(yytree::layout(code = 1, name = "slant"), "disagree")
  expect_length((x + inner_node_annotation(df=data.frame(name1="A",name2="B",text="AB")))$layers,1)
  expect_length((x + inner_node_render(name1="A",name2="B",fill="red"))$layers,1)
  expect_error(x + inner_node_annotation(df=data.frame(name1="A",name2="B")),"text")
})
test_that("Newick labels retain whitespace, punctuation, Unicode and escaped quotes", {
  tree <- yytree("(('Homo sapiens':1e-5,'O''Brien, sample':2)[note]:1,'Mus musculus':3);")
  expect_equal(tree$tree$labels,c("Homo sapiens","O'Brien, sample","Mus musculus"))
  expect_true(1e-5 %in% tree$tree$edges[,3])
  expect_equal(yytree("(Homo sapiens:1,Mus musculus:2);")$tree$labels,c("Homo sapiens","Mus musculus"))
  unicode <- intToUtf8(c(0x6811,0x53F6))
  expect_equal(yytree(paste0("('",unicode,"':1,B:2);"))$tree$labels[1],unicode)
  expect_error(yytree("('unfinished:1,B:2);"),"Unterminated")
})
test_that("matrix colors retain data scale and missing values", {
  m <- matrix(c(0,NA,5,10), 2, dimnames=list(c("A","B"),c("x","y")))
  layer <- leaf_annotation_heatmap(m)
  expect_equal(layer$limits, c(0,10))
  expect_true(is.na(layer$cells[2,1]))
  expect_error(leaf_annotation_heatmap(unname(m)), "rownames")
  category <- leaf_annotation_rect(data.frame(name = "A", group = factor("x", levels=c("x","y"))))
  expect_equal(category$columns$group$levels,c("x","y"))
  expect_equal(yytree:::.color("#12345678"), "#12345678")
  expect_true(is.na(yytree:::.color(NA)))
})
test_that("wire arrays remain arrays even for one row or column", {
  y <- yytree("(A:1,B:2);") + leaf_annotation_rect(data.frame(name="A",group="one"))
  wire <- jsonlite::fromJSON(yytree:::.scene_json(y), simplifyVector = FALSE)
  expect_type(wire$layers[[1]]$names,"list")
  expect_type(wire$layers[[1]]$columns$group$levels,"list")
})
test_that("free element geometry validates controls", {
  expect_error(free_bezier(matrix(1,3,2)),"point")
  expect_error(free_line(0,0,1,1, line_thickness=-1),"thickness")
  p <- rbind(c(0,0),c(.2,.3),c(.8,.7),c(1,1))
  expect_equal(free_bezier_ring(p,p)$type,"bezier_ring")
  expect_equal(free_text(4,12,"hello",mode="percent")$mode,"percent")
})
