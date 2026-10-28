.color <- function(x) {
  if (is.null(x)) return(NULL)
  vapply(x, function(z) if (is.na(z)) NA_character_ else {
    rgb <- grDevices::col2rgb(z, alpha = TRUE)
    grDevices::rgb(rgb[1], rgb[2], rgb[3], rgb[4], maxColorValue = 255)
  }, "", USE.NAMES = FALSE)
}
.legend <- function(x) match.arg(x, c("top_right", "top_left", "bottom_right", "bottom_left", "none"))
leaf_annotation_gap <- function(width = 12) {
  .parameter_number("leaf_annotation_gap", "width", width, 0, Inf,
                    "leaf_annotation_gap(width = 12)")
  .layer("annotation_gap", width = width)
}
leaf_annotation_rect <- function(df, legend = "top_right", colors = NULL, width = 16,
                                 text_rotation = 60, title = NULL) {
  if (!is.data.frame(df) || !"name" %in% names(df) || ncol(df) < 2) stop("df needs name and category columns.")
  columns <- setdiff(names(df), "name")
  .parameter_number("leaf_annotation_rect", "text_rotation", text_rotation, -90, 90,
                    "leaf_annotation_rect(df, text_rotation = 45)")
  if (!is.null(title)) {
    if (is.character(title) && length(title) == 1L && is.null(names(title)) && length(columns) == 1L)
      names(title) <- columns
    if (!is.character(title) || !length(title) || anyNA(title) || is.null(names(title)) ||
        anyNA(names(title)) || any(!nzchar(names(title))) || anyDuplicated(names(title)) ||
        any(!names(title) %in% columns))
      .parameter_error("leaf_annotation_rect", "title", title,
        "A named character vector using unique annotation column names; for one column, a single string is also allowed.",
        "leaf_annotation_rect(df, title = c(group = 'Species'))")
  }
  values <- stats::setNames(lapply(columns, function(column) {
    x <- df[[column]]
    lev <- if (is.factor(x)) levels(x) else unique(as.character(x[!is.na(x)]))
    palette <- if (is.null(colors)) grDevices::hcl.colors(length(lev), "Dark 3") else {
      mapping <- if (is.list(colors)) colors[[column]] else colors
      if (is.null(mapping) || is.null(names(mapping)) || any(!lev %in% names(mapping))) stop("colors must name every category.")
      mapping[lev]
    }
    list(values = as.character(x), levels = lev, colors = .color(palette))
  }), columns)
  .layer("track", names = as.character(df$name), columns = values, legend = .legend(legend),
         width = width, text_rotation = text_rotation, legend_titles = if (is.null(title)) NULL else as.list(title))
}
.heat_colors <- function(x, colors, limits) {
  if (!is.matrix(x) || !is.numeric(x) || !all(dim(x) > 0) || any(is.infinite(x))) stop("Expected a nonempty finite numeric matrix (NA allowed).")
  if (is.null(limits)) limits <- if (all(is.na(x))) c(0, 1) else range(x, na.rm = TRUE)
  if (length(limits) != 2 || any(!is.finite(limits)) || limits[1] > limits[2]) stop("Invalid heatmap limits.")
  palette <- grDevices::colorRampPalette(colors, alpha = TRUE)(256)
  span <- diff(limits)
  clipped <- pmax(limits[1], pmin(limits[2], x))
  if (span == 0) index <- matrix(128, nrow(x), ncol(x)) else {
    if (is.finite(span)) fraction <- (clipped - limits[1]) / span else {
      magnitude <- max(abs(limits))
      fraction <- (clipped / magnitude - limits[1] / magnitude) / (limits[2] / magnitude - limits[1] / magnitude)
    }
    index <- 1 + round(255 * pmax(0, pmin(1, fraction)))
  }
  cells <- matrix(.color(palette[as.vector(index)]), nrow(x), ncol(x))
  cells[is.na(x)] <- NA_character_
  list(cells = cells, limits = limits, palette = .color(palette))
}
leaf_annotation_heatmap <- function(x, legend = "top_right", colors = c("#2166AC", "white", "#B2182B"), limits = NULL, width = 16,
                                    text_rotation = 60, title = NULL) {
  if (is.null(rownames(x))) stop("Heatmap needs leaf names in rownames.")
  .parameter_number("leaf_annotation_heatmap", "text_rotation", text_rotation, -90, 90,
                    "leaf_annotation_heatmap(x, text_rotation = 45)")
  if (!is.null(title) && (!is.character(title) || length(title) != 1L || is.na(title)))
    .parameter_error("leaf_annotation_heatmap", "title", title, "One character string or NULL.",
                     "leaf_annotation_heatmap(x, title = 'Expression')")
  mapped <- .heat_colors(x, colors, limits)
  titles <- colnames(x)
  if (is.null(titles)) titles <- paste0("V", seq_len(ncol(x)))
  .layer("heatmap", names = rownames(x), cells = mapped$cells, titles = titles,
         limits = mapped$limits, palette = mapped$palette, legend = .legend(legend), width = width,
         text_rotation = text_rotation, legend_title = if (is.null(title)) NULL else unname(title))
}
.free <- function(type, mode, fill_color, line_color, line_thickness, line_type, ...) {
  if (length(line_thickness) != 1 || !is.finite(line_thickness) || line_thickness < 0) stop("Invalid line thickness.")
  fields <- list(...)
  numeric <- intersect(names(fields), c("x", "y", "x1", "y1", "x2", "y2", "width", "height", "gap", "size"))
  for (key in numeric) if (!is.numeric(fields[[key]]) || length(fields[[key]]) != 1 || !is.finite(fields[[key]])) stop("Invalid geometry value: ", key)
  for (key in intersect(names(fields), c("width", "height", "size"))) if (fields[[key]] <= 0) stop(key, " must be positive.")
  .layer(type, mode = match.arg(mode, c("relative", "percent", "pixel")),
         fill_color = .color(fill_color), line_color = .color(line_color),
         line_thickness = line_thickness, line_type = match.arg(line_type, c("solid", "dashed", "dotted")), ...)
}
free_text <- function(x, y, text, mode = "relative", locator = "left", gap = 10, color = "black", size = 12) {
  if (!is.character(text) || length(text) != 1 || is.na(text)) stop("text must be a single string.")
  .free("text", mode, color, NA, 0, "solid", x = x, y = y, text = text,
        locator = match.arg(locator, c("left", "right", "center")), gap = gap, size = size)
}
free_ellipse <- function(x, y, width, height, mode = "relative", fill_color = NA, line_color = "black", line_thickness = 1, line_type = "solid") .free("ellipse", mode, fill_color, line_color, line_thickness, line_type, x = x, y = y, width = width, height = height)
free_rectangle <- function(x, y, width, height, mode = "relative", fill_color = NA, line_color = "black", line_thickness = 1, line_type = "solid") .free("rectangle", mode, fill_color, line_color, line_thickness, line_type, x = x, y = y, width = width, height = height)
free_line <- function(x1, y1, x2, y2, mode = "relative", line_color = "black", line_thickness = 1, line_type = "solid") .free("line", mode, NA, line_color, line_thickness, line_type, x1 = x1, y1 = y1, x2 = x2, y2 = y2)
free_arrow <- function(x1, y1, x2, y2, mode = "relative", line_color = "black", line_thickness = 1, line_type = "solid",
                       head_style = "open", head_length = NULL, head_width = NULL) {
  for (key in c("head_length", "head_width")) {
    value <- get(key)
    if (!is.null(value) && (!is.numeric(value) || is.complex(value) || length(value) != 1L || !is.finite(value) || value <= 0))
      stop(key, " must be NULL or one finite positive number.")
  }
  .free("arrow", mode, NA, line_color, line_thickness, line_type,
        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
        head_style = match.arg(head_style, c("open", "filled")), head_length = head_length, head_width = head_width)
}
.points <- function(x, count = NULL) {
  if (!is.matrix(x) || !is.numeric(x) || ncol(x) != 2 || nrow(x) < 2 || any(!is.finite(x)) || (!is.null(count) && nrow(x) != count)) stop("Invalid two-column point matrix.")
  x
}
free_polygon <- function(points, mode = "relative", fill_color = NA, line_color = "black", line_thickness = 1, line_type = "solid") {
  points <- .points(points)
  if (nrow(points) < 3) stop("A polygon needs at least three points.")
  .free("polygon", mode, fill_color, line_color, line_thickness, line_type, points = points)
}
free_bezier <- function(points, mode = "relative", line_color = "black", line_thickness = 1, line_type = "solid") .free("bezier", mode, NA, line_color, line_thickness, line_type, points = .points(points, 4))
free_bezier_ring <- function(points1, points2, mode = "relative", fill_color = "grey", line_color = NA, line_thickness = 1, line_type = "solid") .free("bezier_ring", mode, fill_color, line_color, line_thickness, line_type, points = .points(points1, 4), points2 = .points(points2, 4))
free_heatmap <- function(x, y, width, height, matrix, mode = "relative", colors = c("#2166AC", "white", "#B2182B"), limits = NULL) {
  .free("free_heatmap", mode, NA, NA, 0, "solid", x = x, y = y, width = width, height = height,
        cells = .heat_colors(matrix, colors, limits)$cells,
        source = list(matrix = matrix, colors = .color(colors), limits = limits))
}
