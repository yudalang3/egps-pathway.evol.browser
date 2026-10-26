\name{yytree}
\alias{yytree}
\alias{layout}
\alias{leaf_labels}
\alias{leaf_label_render}
\alias{leaf_render}
\alias{inner_node_render}
\alias{inner2leaf_highlight}
\alias{inner_node_annotation}
\alias{config_yytree}
\alias{quit_yytree}
\alias{leaf_annotation_rect}
\alias{leaf_annotation_heatmap}
\alias{free_text}
\alias{free_ellipse}
\alias{free_rectangle}
\alias{free_line}
\alias{free_arrow}
\alias{free_polygon}
\alias{free_bezier}
\alias{free_bezier_ring}
\alias{free_heatmap}
\alias{print.yytree}
\alias{+.yytree}
\title{Composable interactive eGPS trees}
\description{Build an ordinary R plotting specification and display it in an independent Java Swing window using eGPS Modern Tree View.}
\usage{
yytree(tree, width = 1000, height = 800, blank_top = 20, blank_left = 40, blank_bottom = 80,
    blank_right = 40)

layout(code = NULL, name = NULL)

leaf_labels(show = TRUE, align = c("right", "left", "center"), font_family = NULL, font_size = NULL)

leaf_label_render(df)

leaf_render(size = NULL, fill = NULL, df = NULL)

inner_node_render(size = NULL, fill = NULL, df = NULL, name1 = NULL, name2 = NULL)

inner2leaf_highlight(name1 = NULL, name2 = NULL, fill = "green", df = NULL)

inner_node_annotation(name1 = NULL, name2 = NULL, text = NULL, shape = "rect", size = 20, fill = "pink",
    df = NULL)

config_yytree(path2egps, max_memory_heap = "4g", max_memory_stack = "8m", restart = FALSE)

quit_yytree()

leaf_annotation_rect(df, legend = "top_right", colors = NULL, width = 16)

leaf_annotation_heatmap(x, legend = "top_right", colors = c("#2166AC", "white", "#B2182B"), limits = NULL,
    width = 16)

free_text(x, y, text, mode = "relative", locator = "left", gap = 10, color = "black",
    size = 12)

free_ellipse(x, y, width, height, mode = "relative", fill_color = NA, line_color = "black",
    line_thickness = 1, line_type = "solid")

free_rectangle(x, y, width, height, mode = "relative", fill_color = NA, line_color = "black",
    line_thickness = 1, line_type = "solid")

free_line(x1, y1, x2, y2, mode = "relative", line_color = "black", line_thickness = 1,
    line_type = "solid")

free_arrow(x1, y1, x2, y2, mode = "relative", line_color = "black", line_thickness = 1,
    line_type = "solid")

free_polygon(points, mode = "relative", fill_color = NA, line_color = "black", line_thickness = 1,
    line_type = "solid")

free_bezier(points, mode = "relative", line_color = "black", line_thickness = 1, line_type = "solid")

free_bezier_ring(points1, points2, mode = "relative", fill_color = "grey", line_color = NA, line_thickness = 1,
    line_type = "solid")

free_heatmap(x, y, width, height, matrix, mode = "relative", colors = c("#2166AC", "white",
    "#B2182B"), limits = NULL)

\method{print}{yytree}(x, ..., new_window = FALSE)
\method{+}{yytree}(e1, e2)
}
\arguments{
\item{tree}{A hclust or phylo object, one Newick string, or a Newick file path.}
\item{width}{Canvas width in pixels, free element width in the chosen unit, or track width in pixels.}
\item{height}{Canvas height in pixels or free element height in the chosen coordinate unit.}
\item{blank_top}{Nonnegative canvas margin in logical pixels.}
\item{blank_left}{Nonnegative canvas margin in logical pixels.}
\item{blank_bottom}{Nonnegative canvas margin in logical pixels.}
\item{blank_right}{Nonnegative canvas margin in logical pixels.}
\item{code}{Layout number from 1 to 7.}
\item{name}{Layout name: rect, rect_align, circ, circ_align, arch, arch_align or slant.}
\item{show}{Whether leaf labels are displayed.}
\item{align}{Leaf label column alignment: left, center or right. Circular layouts (3 and 4) use MTV radial tip labels and ignore this argument: labels follow real tips in layout 3 and extended tips in layout 4, include node-radius clearance, and flip on the left half to remain upright.}
\item{font_family}{Optional Java font family. NULL retains the eGPS preference.}
\item{font_size}{Optional leaf label font size in logical pixels.}
\item{df}{A data frame keyed by name, or by name1 and name2 for MRCA selectors.}
\item{size}{Node radius or annotation box size, in logical pixels; text uses pixel font size.}
\item{fill}{R fill color, including alpha. Node styles also color the incoming branch.}
\item{name1}{First leaf name identifying an MRCA.}
\item{name2}{Second distinct leaf name identifying an MRCA.}
\item{text}{Text to display.}
\item{shape}{Node annotation shape: rect, ellipse or triangle.}
\item{path2egps}{The eGPS installation directory containing dependency-egps and its Java runtime.}
\item{max_memory_heap}{Maximum Java heap (-Xmx), for example 4g. Use restart = TRUE to change a running JVM.}
\item{max_memory_stack}{Per-thread Java stack (-Xss), for example 8m. Use restart = TRUE to change it; increasing heap does not increase the stack.}
\item{restart}{Restart the Java subprocess, closing all its windows. Defaults to FALSE.}
\item{legend}{Legend corner: top_right, top_left, bottom_right, bottom_left, or none.}
\item{colors}{Heatmap palette colors, or named category colors (optionally a list keyed by column).}
\item{x}{Horizontal coordinate, or the numeric matrix for leaf_annotation_heatmap.}
\item{limits}{Two numeric heatmap limits. NULL uses the range of the entire matrix; no standardization is performed.}
\item{y}{Vertical coordinate.}
\item{mode}{Coordinate unit: relative (0 to 1), percent (0 to 100), or pixel.}
\item{locator}{Horizontal text anchor: left, center or right.}
\item{gap}{Text anchor gap in logical pixels.}
\item{color}{Text color.}
\item{fill_color}{R fill color; NA disables filling.}
\item{line_color}{R stroke color; NA disables the stroke.}
\item{line_thickness}{Nonnegative stroke width in logical pixels.}
\item{line_type}{solid, dashed or dotted.}
\item{x1}{Start x coordinate.}
\item{y1}{Start y coordinate.}
\item{x2}{End x coordinate.}
\item{y2}{End y coordinate.}
\item{points}{A two-column numeric coordinate matrix. Bezier curves require exactly four rows.}
\item{points1}{Four control points for the first cubic boundary of a ribbon.}
\item{points2}{Four control points for the second boundary, ordered in the same direction.}
\item{matrix}{A numeric matrix for a free-position heatmap; no leaf matching is performed.}
\item{...}{Reserved for the generic print interface.}
\item{new_window}{Open a separate preview rather than replacing the current preview.}
\item{e1}{A yytree plot value.}
\item{e2}{A yytree layer.}
}
\details{Call config_yytree before printing. All previews reuse one Java subprocess. Closing windows only disposes the GUI. quit_yytree closes all windows and exits Java; call config_yytree again before printing. Use restart = TRUE to change a running configuration. After unexpected Java exit, the next print restarts it with the last successful configuration; old window state is lost. In-flight communication failures and interrupts terminate Java without replaying the request. R exit or package unload cleans up the subprocess. Loading the package and constructing plot values do not start Java. Seven layouts preserve input branch lengths; aligned layouts only add display extensions. Add leaf_labels(show = TRUE) before leaf_label_render. Label rendering colors the label background. Leaf annotation rows match leaf names, not row positions. Multiple track layers append outwards. Missing annotations are left blank. Colors follow R RGBA conventions. The origin for free elements is the upper-left corner of the entire logical canvas. Ellipses use their centre; rectangles and free heatmaps use their upper-left corner. Canvas dimensions are logical pixels. Previews initially fit the full canvas without enlarging small plots and follow window resizing until manual zoom using the wheel or percentage controls. Afterwards resizing preserves manual zoom and scrollbars appear only on overflow. Printing again restores automatic fit. Fit frame clears panning and restores automatic fitting to the current window, retaining hover switches. Width and Height percentage controls are linked, support decimal input, Enter or focus-loss commit and 1-point steps. Their fixed 100 percent baseline is the first fitted display, also used by the bottom scale readout. Resizing and Fit frame do not change that baseline, so fitting a resized window may not show 100 percent. Entering 100 restores the original display scale. Numeric zoom anchors the visible centre; wheel zoom anchors the pointer. Invalid input retains the last valid view; allowed bounds are shown in tooltips. Controls wrap in narrow windows. Printing establishes a new baseline. GUI zoom does not change the R value or export dimensions. Show Crosshair and Snap To Point default to on in each independent preview. Nodes glow within 12 interface pixels and standard Swing tooltips report their identity, type, incoming branch length and logical position. Snapping aligns the crosshair to the nearest node. Turning snapping or guides off retains node inspection. Mouse and Snap status rows distinguish pointer and node coordinates. Dragging hides hover overlays. Viewport images render in the background and are reused during hover. Overlays are excluded from exports and switches reset on printing. These interactions do not launch or attach to the MTV desktop. The current preview is reused by default; print(x, new_window = TRUE) opens another independent view. Node size is radius; annotation size is box width.}
\value{yytree returns a yytree object; layer functions return a yytree_layer. config_yytree invisibly returns effective runtime configuration, including PID and log path. quit_yytree invisibly returns NULL. Printing invisibly returns the unchanged plot.}
\examples{
y <- yytree(hclust(dist(USArrests))) + yytree::layout(name = "slant") +
  leaf_labels(show = TRUE)
stopifnot(inherits(y, "yytree"))
}
