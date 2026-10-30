# Ordinary R values deliberately contain no Java references.
yytree <- function(tree, width = 1000, height = 800,
                   blank_top = 20, blank_left = 40,
                   blank_bottom = 80, blank_right = 40) {
  dims <- c(width, height)
  margins <- c(blank_top, blank_left, blank_bottom, blank_right)
  if (length(dims) != 2L || length(margins) != 4L || !is.numeric(dims) || !is.numeric(margins) ||
      any(!is.finite(dims)) || any(dims <= 0) || any(dims != floor(dims)) ||
      any(!is.finite(margins)) || any(margins < 0))
    stop("Canvas dimensions must be positive and margins nonnegative.")
  if (is.character(tree) && length(tree) == 1L && !is.na(tree)) {
    text <- if (file.exists(tree)) paste(readLines(tree, warn = FALSE, encoding = "UTF-8"), collapse = "\n") else tree
    tree <- .read_newick(text)
    if (is.null(tree) || inherits(tree, "multiPhylo")) stop("Supply exactly one Newick tree.")
  }
  if (inherits(tree, "hclust")) {
    n <- nrow(tree$merge) + 1L
    labels <- tree$labels
    if (is.null(labels)) labels <- as.character(seq_len(n))
    edges <- do.call(rbind, lapply(seq_len(n - 1L), function(i) {
      child <- tree$merge[i, ]
      child_id <- ifelse(child < 0, -child, n + child)
      child_height <- vapply(child, function(k) if (k < 0) 0 else tree$height[k], 0.0)
      cbind(parent = n + i, child = child_id, length = tree$height[i] - child_height)
    }))
    tree <- list(labels = labels, edges = edges, root = 2L * n - 1L,
                 order = tree$order)
  } else if (inherits(tree, "phylo")) {
    lengths <- tree$edge.length
    if (is.null(lengths)) lengths <- rep(1, nrow(tree$edge))
    labels <- tree$tip.label
    if (is.null(labels)) labels <- as.character(seq_along(setdiff(tree$edge[, 2], tree$edge[, 1])))
    node_labels <- tree$node.label
    tree <- list(labels = labels,
                 node_labels = if (is.null(node_labels)) NULL else data.frame(id = length(labels) + seq_along(node_labels), name = node_labels),
                 edges = cbind(tree$edge, lengths),
                 root = setdiff(tree$edge[, 1], tree$edge[, 2]))
  } else {
    stop("Expected hclust, phylo, a Newick string or a Newick file.")
  }
  if (!anyNA(tree$labels) && all(!nzchar(tree$labels))) tree$labels <- as.character(seq_along(tree$labels))
  if (anyNA(tree$labels) || any(!nzchar(tree$labels)) || anyDuplicated(tree$labels))
    stop("Leaf names must be nonempty and unique.")
  if (any(!is.finite(tree$edges)) || any(tree$edges[, 3] < 0))
    stop("Tree contains invalid or negative branch lengths.")
  .validate_tree(tree)
  structure(list(schema_version = 1L, tree = tree,
                 canvas = list(width = width, height = height, margins = margins),
                 layers = list()), class = "yytree")
}

# Protect names before ape removes whitespace. Tokens are replaced only in label positions,
# so scientific branch lengths and bracket comments keep their original meaning.
.read_newick <- function(text) {
  chars <- strsplit(text, "", fixed = TRUE)[[1]]
  out <- character(length(chars))
  labels <- character()
  i <- 1L
  count <- 0L
  expect_label <- TRUE
  while (i <= length(chars)) {
    ch <- chars[i]
    token <- ch
    if (ch == "[") {
      start <- i
      depth <- 1L
      while (depth > 0L && i < length(chars)) {
        i <- i + 1L
        if (chars[i] == "[") depth <- depth + 1L
        if (chars[i] == "]") depth <- depth - 1L
      }
      if (depth) stop("Unterminated Newick comment.")
      token <- paste0(chars[start:i], collapse = "")
    } else if (ch %in% c("(", ",", ")")) {
      expect_label <- TRUE
    } else if (ch %in% c(":", ";")) {
      expect_label <- FALSE
    } else if (expect_label && !grepl("^[[:space:]]$", ch)) {
      if (ch %in% c("'", '"')) {
        quote <- ch
        name <- character()
        closed <- FALSE
        i <- i + 1L
        while (i <= length(chars)) {
          if (chars[i] == quote) {
            if (i < length(chars) && chars[i + 1L] == quote) {
              name <- c(name, quote)
              i <- i + 2L
              next
            }
            closed <- TRUE
            break
          }
          name <- c(name, chars[i])
          i <- i + 1L
        }
        if (!closed) stop("Unterminated quoted Newick label.")
        name <- paste0(name, collapse = "")
      } else {
        start <- i
        while (i < length(chars) && !chars[i + 1L] %in% c("(", ")", ",", ":", ";", "[")) i <- i + 1L
        name <- trimws(paste0(chars[start:i], collapse = ""))
      }
      labels <- c(labels, name)
      token <- paste0("YYTREE_NAME_", length(labels))
      expect_label <- FALSE
    }
    count <- count + 1L
    out[count] <- token
    i <- i + 1L
  }
  tree <- ape::read.tree(text = paste0(out[seq_len(count)], collapse = ""))
  if (is.null(tree) || inherits(tree, "multiPhylo")) return(tree)
  restore <- function(names) {
    keys <- paste0("YYTREE_NAME_", seq_along(labels))
    index <- match(names, keys)
    present <- !is.na(index)
    names[present] <- labels[index[present]]
    names
  }
  tree$tip.label <- restore(tree$tip.label)
  if (!is.null(tree$node.label)) tree$node.label <- restore(tree$node.label)
  tree
}

.validate_tree <- function(tree) {
  edges <- tree$edges
  if (!is.matrix(edges) || ncol(edges) != 3 || !length(tree$labels)) stop("Malformed tree edges.")
  ids <- edges[, 1:2, drop = FALSE]
  if (any(ids < 1 | ids != floor(ids)) || anyDuplicated(edges[, 2])) stop("Invalid node identifiers or multiple parents.")
  roots <- setdiff(edges[, 1], edges[, 2])
  if (length(roots) != 1 || length(tree$root) != 1 || roots != tree$root) stop("Tree must have exactly one root.")
  tips <- setdiff(edges[, 2], edges[, 1])
  if (!setequal(tips, seq_along(tree$labels))) stop("Leaf identifiers do not match labels.")
  nodes <- unique(as.vector(ids))
  children <- split(match(ids[, 2], nodes),
                    factor(match(ids[, 1], nodes), levels = seq_along(nodes)))
  pending <- integer(length(nodes))
  pending[1] <- match(tree$root, nodes)
  visited <- logical(length(nodes))
  first <- last <- 1L
  while (first <= last) {
    node <- pending[first]
    first <- first + 1L
    if (visited[node]) stop("Tree contains a cycle.")
    visited[node] <- TRUE
    descendants <- children[[node]]
    if (length(descendants)) {
      pending[last + seq_along(descendants)] <- descendants
      last <- last + length(descendants)
    }
  }
  if (!all(visited)) stop("Tree contains disconnected nodes.")
  invisible(TRUE)
}

.layer <- function(type, ...) {
  fields <- list(type = type, ...)
  structure(fields[!vapply(fields, is.null, FALSE)], class = "yytree_layer")
}

.parameter_error <- function(fun, parameter, value, requirement, example) {
  received <- paste(utils::head(utils::capture.output(utils::str(value, give.attr = FALSE, max.level = 1)), 4), collapse = " ")
  message <- paste0("Invalid ", fun, "() parameter '", parameter, "'.\nReceived: ",
                    substr(received, 1, 400), "\nExpected: ", requirement, "\nExample: ", example)
  .notify_parameter_error(message)
  stop(message, call. = FALSE)
}

.parameter_number <- function(fun, parameter, value, minimum, maximum, example,
                              integer = FALSE, lower_open = FALSE, upper_open = FALSE) {
  valid <- is.numeric(value) && !is.complex(value) && length(value) == 1L && is.finite(value)
  if (valid) valid <- (if (lower_open) value > minimum else value >= minimum) &&
    (if (upper_open) value < maximum else value <= maximum) && (!integer || value == floor(value))
  if (!valid) .parameter_error(fun, parameter, value,
    paste0("One finite ", if (integer) "integer" else "number", " in ",
           if (lower_open) "(" else "[", minimum, ", ", maximum,
           if (upper_open) ")." else "]."), example)
}

.parameter_dots <- function(fun, dots, supported, example) {
  if (length(dots)) .parameter_error(fun, "...", dots,
    paste0("Only these named parameters are supported: ", paste(supported, collapse = ", "), "."), example)
}

layout <- function(code = NULL, name = NULL, start_angle = NULL, angle_extent = NULL,
                   tree_width_ratio = NULL, left_margin_ratio = NULL, direction = NULL, ...) {
  example <- "yytree::layout(code = 3, start_angle = 330, angle_extent = 90)"
  .parameter_dots("layout", list(...), c("code", "name", "start_angle", "angle_extent",
    "tree_width_ratio", "left_margin_ratio", "direction"), example)
  names <- c("rect", "rect_align", "circ", "circ_align", "arch", "arch_align", "slant")
  if (!is.null(code) && (!is.numeric(code) || is.complex(code) || length(code) != 1L || !is.finite(code) || !code %in% seq_len(7)))
    .parameter_error("layout", "code", code, "One integer from 1 through 7.", "yytree::layout(code = 7)")
  if (!is.null(name)) {
    if (!is.character(name) || length(name) != 1L || is.na(name))
      .parameter_error("layout", "name", name, paste(names, collapse = ", "), example)
    name <- switch(name, rectangle = "rect", circular = "circ", name)
    matched <- match(name, names)
    if (is.na(matched)) .parameter_error("layout", "name", name, paste(names, collapse = ", "), example)
    if (!is.null(code) && code != matched) .parameter_error("layout", "code/name", list(code = code, name = name),
      "Layout code and name must agree; the supplied values disagree.", "yytree::layout(code = 7, name = 'slant')")
    code <- matched
  }
  if (is.null(code)) code <- 1L
  if (!is.null(direction)) {
    direction_example <- "yytree::layout(code = 7, direction = 'up')"
    if (code != 7L) .parameter_error("layout", "direction", direction,
      "This parameter applies only to slanted layout 7.", direction_example)
    if (!is.character(direction) || length(direction) != 1L || is.na(direction) ||
        !direction %in% c("right", "down", "left", "up"))
      .parameter_error("layout", "direction", direction,
        "One of 'right', 'down', 'left', or 'up' (root-to-tip direction).", direction_example)
  }
  values <- list(start_angle = start_angle, angle_extent = angle_extent,
                 tree_width_ratio = tree_width_ratio, left_margin_ratio = left_margin_ratio)
  for (key in names(values)) if (!is.null(values[[key]])) {
    circular <- key %in% c("start_angle", "angle_extent")
    if ((circular && !code %in% c(3L, 4L)) || (!circular && code != 7L))
      .parameter_error("layout", key, values[[key]],
        if (circular) "This parameter applies only to circular layouts 3 and 4."
        else "This parameter applies only to slanted layout 7.",
        if (circular) example else "yytree::layout(code = 7, tree_width_ratio = 0.8, left_margin_ratio = 0.1)")
    .parameter_number("layout", key, values[[key]],
      if (key == "angle_extent") 1 else 0, if (circular) 360 else 1,
      if (circular) example else "yytree::layout(code = 7, tree_width_ratio = 0.8, left_margin_ratio = 0.1)",
      integer = circular, lower_open = key == "tree_width_ratio", upper_open = key == "left_margin_ratio")
  }
  .layer("layout", code = as.integer(code), start_angle = start_angle, angle_extent = angle_extent,
         tree_width_ratio = tree_width_ratio, left_margin_ratio = left_margin_ratio, direction = direction)
}

leaf_labels <- function(show = TRUE, align = c("right", "left", "center"),
                        font_family = NULL, font_size = NULL, gap_length = 6, ...) {
  example <- "leaf_labels(align = 'left', gap_length = 6)"
  .parameter_dots("leaf_labels", list(...), c("show", "align", "font_family", "font_size", "gap_length"), example)
  if (!is.logical(show) || length(show) != 1 || is.na(show))
    .parameter_error("leaf_labels", "show", show, "One TRUE or FALSE value.", example)
  if (missing(align)) align <- "right"
  if (!is.character(align) || length(align) != 1L || is.na(align) || !align %in% c("right", "left", "center"))
    .parameter_error("leaf_labels", "align", align, "One of 'left', 'center', or 'right'.", example)
  if (!is.null(font_family) && (!is.character(font_family) || length(font_family) != 1 || is.na(font_family)))
    .parameter_error("leaf_labels", "font_family", font_family, "A single font name.", "leaf_labels(font_family = 'Serif')")
  if (!is.null(font_size)) .parameter_number("leaf_labels", "font_size", font_size, 0, Inf,
    "leaf_labels(font_size = 14)", lower_open = TRUE)
  .parameter_number("leaf_labels", "gap_length", gap_length, 0, Inf, example)
  .layer("leaf_labels", show = show, align = align, font_family = font_family, font_size = font_size, gap_length = gap_length)
}

leaf_label_render <- function(df) .layer("leaf_label_render", df = df)
leaf_render <- function(size = NULL, fill = NULL, df = NULL) .layer("leaf_render", size = size, fill = fill, df = df)
inner_node_render <- function(size = NULL, fill = NULL, df = NULL, name1 = NULL, name2 = NULL) .layer("inner_node_render", size = size, fill = fill, df = df, name1 = name1, name2 = name2)
.annotation_text_parameters <- function(fun, text, text_position, df) {
  example <- paste0(fun, "('A', 'B', text = 'Clade', text_position = 'top')")
  positions <- c("left", "right", "top", "bottom", "topleft", "topright", "bottomleft", "bottomright")
  position_requirement <- paste0("One of ", paste(shQuote(positions), collapse = ", "), ".")
  if (!is.null(text) && (!is.character(text) || length(text) != 1L || is.na(text)))
    .parameter_error(fun, "text", text, "One nonmissing string or NULL.", example)
  if (!is.character(text_position) || length(text_position) != 1L || is.na(text_position) || !text_position %in% positions)
    .parameter_error(fun, "text_position", text_position, position_requirement, example)
  if (is.data.frame(df)) {
    if ("text" %in% names(df) && (!is.character(df$text) || anyNA(df$text)))
      .parameter_error(fun, "df$text", df$text, "Nonmissing character strings.", example)
    if ("text_position" %in% names(df) && (!is.character(df$text_position) || anyNA(df$text_position) || any(!df$text_position %in% positions)))
      .parameter_error(fun, "df$text_position", df$text_position, position_requirement, example)
  }
}
inner2leaf_highlight <- function(name1 = NULL, name2 = NULL, fill = "green", df = NULL,
                                 text = NULL, text_position = "left") {
  .annotation_text_parameters("inner2leaf_highlight", text, text_position, df)
  layer <- .layer("inner2leaf_highlight", name1 = name1, name2 = name2, fill = fill, df = df,
                  text = text, text_position = text_position)
  # Keep NULL explicit so R's $text cannot partially match text_position.
  layer["text"] <- list(text)
  layer
}
inner_node_annotation <- function(name1 = NULL, name2 = NULL, text = NULL, shape = "rect", size = 20, fill = "pink", df = NULL,
                                   text_position = "right") {
  .annotation_text_parameters("inner_node_annotation", text, text_position, df)
  layer <- .layer("inner_node_annotation", name1 = name1, name2 = name2, text = text, shape = shape,
                  size = size, fill = fill, df = df, text_position = text_position)
  layer["text"] <- list(text)
  layer
}

`+.yytree` <- function(e1, e2) {
  if (!inherits(e2, "yytree_layer")) stop("Only yytree layers can be added.")
  if (e2$type == "leaf_label_render") {
    visible <- FALSE
    for (layer in e1$layers) if (layer$type == "leaf_labels") visible <- layer$show
    if (!visible) stop("Add leaf_labels(show = TRUE) before leaf_label_render().")
  }
  if (e2$type == "interact") {
    known <- c(e1$tree$labels, e1$tree$node_labels$name)
    for (key in e2$data$name) {
      count <- sum(!is.na(known) & known == key)
      if (count == 0) stop("Unknown node name: ", key)
      if (count > 1) stop("Ambiguous duplicate node name: ", key)
    }
    e1$layers <- Filter(function(x) x$type != "interact", e1$layers)
  }
  if (!is.null(e2$df)) {
    if (!is.data.frame(e2$df) || !nrow(e2$df)) stop("df must be a nonempty data frame.")
    required <- if (e2$type %in% c("inner_node_render", "inner2leaf_highlight", "inner_node_annotation")) c("name1", "name2") else "name"
    if (!all(required %in% names(e2$df))) stop("Missing selector columns: ", paste(required, collapse = ", "))
    columns <- intersect(c("name", "name1", "name2"), names(e2$df))
    for (column in columns) if (any(!e2$df[[column]] %in% e1$tree$labels)) stop("Unknown leaf name.")
    if (anyDuplicated(e2$df[required])) stop("Duplicate annotation selectors.")
    if (length(required) == 2 && any(e2$df$name1 == e2$df$name2)) stop("MRCA requires distinct leaves.")
    if (e2$type == "leaf_label_render" && !"color" %in% names(e2$df)) stop("Label rendering requires a color column.")
    if ("size" %in% names(e2$df) && (!is.numeric(e2$df$size) || any(!is.finite(e2$df$size)) || any(e2$df$size < 0))) stop("Invalid node size.")
    for (column in intersect(c("fill", "color"), names(e2$df))) .color(e2$df[[column]])
  }
  if (e2$type %in% c("track", "heatmap")) {
    if (anyNA(e2$names) || anyDuplicated(e2$names) || any(!e2$names %in% e1$tree$labels)) stop("Annotation names must be unique known leaves.")
    if (!is.numeric(e2$width) || length(e2$width) != 1 || !is.finite(e2$width) || e2$width <= 0) stop("Track width must be positive.")
  }
  targeted_inner <- e2$type == "inner_node_render" && (!is.null(e2$name1) || !is.null(e2$name2))
  if ((e2$type %in% c("inner2leaf_highlight", "inner_node_annotation") || targeted_inner) && is.null(e2$df)) {
    if (length(e2$name1) != 1 || length(e2$name2) != 1 || !e2$name1 %in% e1$tree$labels || !e2$name2 %in% e1$tree$labels || e2$name1 == e2$name2) stop("Supply two distinct known leaves for MRCA.")
  }
  if (!is.null(e2$size) && (any(!is.finite(e2$size)) || any(e2$size < 0))) stop("size must be nonnegative.")
  if (e2$type == "inner_node_annotation") {
    shapes <- if (!is.null(e2$df$shape)) e2$df$shape else e2$shape
    if (any(!shapes %in% c("rect", "ellipse", "triangle"))) stop("Unknown annotation shape.")
    texts <- if (!is.null(e2$df[["text"]])) e2$df[["text"]] else e2[["text"]]
    if (is.null(texts) || anyNA(texts) || (!is.null(e2[["text"]]) && length(e2[["text"]]) != 1)) stop("Supply text, or a text column in df.")
  }
  if (!is.null(e2$fill)) .color(e2$fill)
  e1$layers <- c(e1$layers, list(e2))
  e1
}

interact <- function(df) {
  if (!is.data.frame(df) || !nrow(df) || !"name" %in% names(df)) stop("df must be a nonempty data frame with a name column.")
  if (anyNA(names(df)) || any(!nzchar(names(df))) || anyDuplicated(names(df))) stop("Column names must be nonempty and unique.")
  if (!is.character(df$name) && !is.factor(df$name)) stop("name must be character or factor.")
  if (anyNA(df$name) || any(!nzchar(as.character(df$name))) || anyDuplicated(df$name)) stop("Node names must be nonempty and unique.")
  for (key in names(df)) {
    x <- df[[key]]
    if (is.list(x) || !is.null(dim(x)) || !(is.character(x) || is.numeric(x) || is.logical(x) || is.factor(x) || inherits(x, "Date") || inherits(x, "POSIXt"))) stop("Unsupported tooltip column: ", key)
    missing <- is.na(x)
    df[[key]] <- as.character(x)
    df[[key]][missing] <- "\u2014"
  }
  .layer("interact", data = df)
}
