# Run from the repository root. This is the source for rPackage/man/*.Rd.
# Documentation is function-specific; signatures come from the implementation.
implementation <- new.env(parent = globalenv())
for (file in list.files("rPackage/R", pattern = "\\.R$", full.names = TRUE))
  sys.source(file, envir = implementation)

function_docs <- list()
document <- function(name, description, arguments, details, examples,
                     value = "A \\code{yytree_layer} value; add it to a plot with \\code{+}.",
                     usage = NULL) {
  actual <- names(formals(get(name, envir = implementation)))
  if (!identical(names(arguments), actual))
    stop("Missing, extra or out-of-order argument documentation for ", name)
  if (anyNA(arguments) || any(!nzchar(arguments))) stop("Empty argument documentation for ", name)
  function_docs[[name]] <<- list(description = description, arguments = arguments,
    details = details, examples = examples, value = value, usage = usage)
}

# Shared text is reused only where its meaning is identical.
color_rule <- r"(An R color name, \code{#RRGGBB} or \code{#RRGGBBAA}; alpha is supported.)"
coordinate_rule <- r"(Coordinates refer to the entire logical canvas, including margins, not to tree or branch coordinates. The origin is the upper-left corner; x increases rightwards and y downwards. Values outside the canvas are not clamped.)"
mode_arg <- r"(One string: \code{"relative"} (default; canvas width and height are 1), \code{"percent"} (width and height are 100), or \code{"pixel"} (logical pixels). Only positions and geometric widths/heights use this unit; font sizes, gaps and stroke widths remain logical pixels.)"
fill_arg <- paste(r"(Fill color. Default \code{NA} leaves the interior unfilled.)", color_rule)
stroke_args <- c(
  line_color = paste(r"(Stroke color. Default \code{"black"}; \code{NA} disables the stroke.)", color_rule),
  line_thickness = "One finite nonnegative stroke width in logical pixels. Default 1; 0 hides the stroke.",
  line_type = r"(One string: \code{"solid"} (default), \code{"dashed"}, or \code{"dotted"}.)")
heat_colors_arg <- paste(r"(Vector of R colors interpolated into a continuous palette. Default \code{c("#2166AC", "white", "#B2182B")} (blue, white, red).)", color_rule)
limits_arg <- r"(Two finite numbers in increasing or equal order. Default \code{NULL} uses the range of all nonmissing entries in the entire matrix, or \code{c(0, 1)} for an all-missing matrix. Values outside the limits are clipped; equal limits use the palette midpoint. No automatic standardization or Z-score is applied.)"
mrca_arg <- r"(One leaf-name string, default \code{NULL}. To select a single node without \code{df}, supply both \code{name1} and \code{name2}: they must be distinct known leaves and identify their most recent common ancestor (MRCA). With \code{df}, each row supplies both selector columns.)"
node_size_arg <- r"(Finite nonnegative node radius in logical pixels, not diameter. Default \code{NULL} leaves the existing radius unchanged; initially nodes have radius 0 (no visible node symbol). A supplied \code{df$size} overrides this value for its row.)"
node_fill_arg <- paste(r"(Color for both the node symbol and its incoming branch. Default \code{NULL} retains the existing color (initially black). \code{NA} makes them transparent. A supplied \code{df$fill} overrides this value for its row.)", color_rule)
selector_rule <- r"(Annotation rows are matched by leaf names, never by row position. Unknown names and duplicate selector rows are rejected when adding the layer.)"

document("yytree", "Create a plot specification from one tree without starting Java or opening a window.",
  c(tree = r"(Required. One \code{hclust} or \code{ape::phylo} object, one Newick string, or one Newick file path. A character value naming an existing file is read as UTF-8; otherwise it is parsed as Newick. Supply exactly one tree.)",
    width = "One finite positive integer: logical canvas width in pixels. Default 1000.",
    height = "One finite positive integer: logical canvas height in pixels. Default 800.",
    blank_top = "Finite nonnegative top margin in logical pixels. Default 20.",
    blank_left = "Finite nonnegative left margin in logical pixels. Default 40.",
    blank_bottom = "Finite nonnegative bottom margin in logical pixels. Default 80.",
    blank_right = "Finite nonnegative right margin in logical pixels. Default 40."),
  r"(The initial display uses rectangular layout 1 and hides leaf labels. Add \code{leaf_labels()} to show them.

Input leaf order is retained. \code{hclust} branch lengths are parent minus child merge heights; negative or nonfinite lengths are rejected. A \code{phylo} without branch lengths uses length 1 for each edge. Newick labels retain spaces, Unicode and quoted punctuation. Leaf names must be nonempty and unique; an entirely unnamed tree receives numeric names. Trees must be connected with one root and nonnegative finite branch lengths.

Widths, heights and margins describe the logical canvas, not the preview window. Space for labels, tracks and legends is reserved in addition to these margins. If insufficient drawing space remains, printing reports an error; increase the canvas dimensions.

The result is an ordinary serializable R value. Creating or saving it does not require a configured Java runtime.)",
  'tree <- "((A:1,B:2):1,C:3);"\np <- yytree(tree)\nq <- yytree(hclust(dist(USArrests)), width = 1200) + leaf_labels()\nstopifnot(inherits(p, "yytree"), length(p$layers) == 0)\n# Displaying a plot requires config_yytree(); see ?print.yytree.',
  "A \\code{yytree} value containing the tree, logical canvas and an initially empty layer list.")

document("layout", "Select the tree layout for a yytree plot.",
  c(code = r"(One numeric integer from 1 through 7. Default \code{NULL}: use \code{name} if supplied, otherwise code 1 (\code{"rect"}). See the layout table below.)",
    name = r"(One layout-name string from the table below. Default \code{NULL}: use \code{code} if supplied, otherwise \code{"rect"}. \code{"rectangle"} aliases \code{"rect"}; \code{"circular"} aliases \code{"circ"}.)",
    start_angle = r"(Circular layouts 3 and 4 only. One integer from 0 through 360, in degrees. Default \code{NULL} uses 285. Zero points right; increasing angles run counterclockwise. Sets the first leaf angle.)",
    angle_extent = r"(Circular layouts 3 and 4 only. One integer from 1 through 360, in degrees. Default \code{NULL} uses 360. Leaves occupy this angular span; values below 360 leave an opening. A full circle spaces leaves by extent divided by leaf count; an open circle includes both endpoints. For a single leaf, its track cell uses the chosen span centered on that leaf.)",
    tree_width_ratio = r"(Slanted layout 7 only. One finite number greater than 0 and at most 1. Default \code{NULL} uses 1. Scales the whole tree along its root-to-tip axis around the working-area center, without changing topology. For right/left this controls width; for up/down it controls height. The parameter name remains the same for all directions.)",
    left_margin_ratio = r"(Slanted layout 7 only. One finite number at least 0 and below 1. Default \code{NULL} uses 0.2. Reserves this fraction of the layout's root-to-tip working span before the tree is fitted and centered; larger values shorten that span. It applies to height for up/down. The historical name is retained; this is not a fixed left-side canvas margin in pixels.)",
    direction = r"(Slanted layout 7 only. One of \code{"right"}, \code{"down"}, \code{"left"}, or \code{"up"}, indicating the direction from root to tips. Default \code{NULL} uses \code{"right"}. Directions correspond to clockwise rotations of 0, 90, 180 and 270 degrees. An explicit non-NULL value on another layout raises an explanatory error. See Details for leaf order, labels and annotations.)",
    "..." = r"(Reserved for detecting unknown arguments. No additional parameters are accepted; misspelled names raise an explanatory error.)"),
  r"(\subsection{Available layouts}{
\tabular{rll}{
Code \tab Name \tab Display\cr
1 \tab \code{rect} \tab Rectangular phylogram\cr
2 \tab \code{rect_align} \tab Rectangular, aligned tips\cr
3 \tab \code{circ} \tab Circular phylogram\cr
4 \tab \code{circ_align} \tab Circular, aligned tips\cr
5 \tab \code{arch} \tab Arch phylogram\cr
6 \tab \code{arch_align} \tab Arch, aligned outer contour\cr
7 \tab \code{slant} \tab Slanted cladogram (topology only)
}

Layouts 1 through 6 draw real branch lengths. An arch has two straight sides joined by a semicircular top and remains open at the bottom. Arch layouts reserve label and annotation thickness above and beside the tree; below the open ends they reserve only the required label and node-marker clearance. The core arch is fitted to the remaining width and height. The aligned variants add dashed extensions to align tips on the layout's outer contour.
}
\subsection{Defaults and parameter compatibility}{
\code{layout()} selects \code{code = 1}, equivalent to \code{name = "rect"}. The plot also uses this layout if no layout layer is added.

\code{code} and \code{name} are not mutually exclusive. Supply either one, or supply both with matching meanings. For example, \code{code = 3, name = "circular"} is valid; \code{code = 1, name = "slant"} raises an error. Unknown names and codes outside 1 through 7 raise errors.

Explicit layout-specific parameters must apply to the selected layout: for example, \code{layout(code = 7, start_angle = 90)} raises an error explaining that angles apply only to layouts 3 and 4. Parameter errors report the received value, allowed values and a corrected example. In interactive R with an already running yytree JVM, a nonmodal explanation dialog also appears unless \code{YYTREE_EXAMPLES_NO_GUI=1}. Batch sessions and sessions without a running JVM only raise the R error; an error never starts Java.
}
\subsection{Composition and interpretation}{
When several layout layers are added, the last one determines the display and resets omitted layout-specific parameters to their defaults. Layout selection does not modify the input tree or stored branch lengths. Aligned layouts add display extensions to a shared outer contour; the extensions are not evolutionary distances. \code{slant} retains stored lengths but draws topology rather than distances.

Circular layouts 3 and 4 use radial tip labels and ignore \code{leaf_labels(align = ...)}. Layout 3 places labels beyond real tips; layout 4 places them beyond extended tips.
}
\subsection{Slanted directions}{
\tabular{llll}{
Direction \tab Root to tips \tab Clockwise rotation \tab First leaf location\cr
\code{right} \tab Rightward \tab 0 degrees \tab Top\cr
\code{down} \tab Downward \tab 90 degrees \tab Right\cr
\code{left} \tab Leftward \tab 180 degrees \tab Bottom\cr
\code{up} \tab Upward \tab 270 degrees \tab Left
}

Directions rotate the complete tree, rather than mirroring it to preserve screen order. For example, leftward trees reverse the top-to-bottom screen order. The R tree's original leaf order, topology and branch lengths remain unchanged. A later \code{layout(code=7)} resets an earlier direction to right.

Labels follow their own tips. Right/left labels remain horizontal and readable; down labels read top-to-bottom after a 90-degree clockwise rotation, and up labels read bottom-to-top after a 90-degree counterclockwise rotation. Text is never upside down. \code{leaf_labels(align=...)} still uses the longest-label region, with left/center/right interpreted in the text's reading direction. \code{gap_length} measures outward clearance along the root-to-tip direction, including node-symbol radius.

Categorical tracks, heatmaps, clade highlights and node annotations follow the tree direction. Track rows still match leaf names, and titles remain readable. Legends stay in their chosen canvas corners. Free elements retain canvas coordinates and do not rotate with the tree. Canvas width, height and the four blank margins retain their screen-coordinate meanings. Layout reserves label and track space on the tip-facing side.

For circular layouts, use \code{start_angle} and \code{angle_extent}, not \code{direction}; for example, \code{layout(code=3, start_angle=330, angle_extent=270)}.
}
\subsection{Name conflict}{
This function masks \code{graphics::layout}, which arranges base-R plotting panels. Use \code{yytree::layout()} explicitly for tree layouts and \code{graphics::layout()} for base graphics.
})",
  'default <- yytree::layout()\nby_code <- yytree::layout(code = 3)\nby_name <- yytree::layout(name = "circ")\nmatching <- yytree::layout(code = 3, name = "circular")\nstopifnot(default$code == 1L, identical(by_code, by_name),\n          identical(by_name, matching))\n# Conflicting arguments raise an error.\nconflict <- tryCatch(yytree::layout(code = 1, name = "slant"),\n                     error = conditionMessage)\nstopifnot(grepl("disagree", conflict))\n# The last layout wins when this specification is displayed.\np <- yytree("((A:1,B:2):1,C:3);") +\n  yytree::layout(name = "rect") + yytree::layout(name = "slant")')

document("leaf_labels", "Show or hide leaf labels and set their alignment and font.",
  c(show = r"(One nonmissing logical value. Default \code{TRUE} shows labels; \code{FALSE} hides them. A plot with no \code{leaf_labels} layer initially hides labels.)",
    align = r"(One of \code{"right"} (effective default), \code{"left"}, or \code{"center"}. The formal default lists the available choices; omitting it selects \code{"right"}. In layouts 1, 2, 5, 6 and 7, every leaf-label region has the same length along the text's reading axis: the longest label measured using the current font. This parameter aligns text left, center or right inside that region, as read by the viewer. In slanted up/down layouts, text and its region rotate 90 degrees together; left/right still mean the beginning/end of the reading axis, not the screen's left/right sides. It does not align tree tips or change branch lengths. Each region follows its own displayed tip, so unequal tips need not produce one straight text column. Circular layouts 3 and 4 ignore this argument and use readable radial labels; see Details.)",
    font_family = r"(One Java font-family string. Default \code{NULL} uses the eGPS font preference, falling back to Java SansSerif when unavailable. Java may substitute an unavailable family.)",
    font_size = r"(One finite positive number: font size in logical pixels. Default \code{NULL} retains the eGPS default label-font size.)",
    gap_length = r"(One finite nonnegative number in logical pixels; default 6. Controls the clearance from the displayed tip's node-symbol edge to the near edge of its equal-width label region, measured outward from the tree. Without a node symbol, clearance starts at the tip. Layouts 2, 4 and 6 use the end of the dashed extension. Circular layouts measure this clearance radially to the text instead. It is independent of branch-length units and scales together with the canvas when zooming.)",
    "..." = r"(Reserved for detecting unknown arguments. No additional parameters are accepted.)"),
  r"(The last \code{leaf_labels} layer replaces all label settings, resetting omitted font settings and \code{gap_length} to their defaults. Labels follow their own real tips, or extended tips in aligned layouts. This also applies to existing scripts that omit \code{gap_length}.

\strong{Alignment relative to the longest label:} Suppose the longest label is 100 logical pixels wide and a shorter label is 40 pixels wide. Every label region is 100 pixels wide. With \code{align="left"}, the shorter label starts at the region's left edge. With \code{align="center"}, it starts 30 pixels farther right. With \code{align="right"}, it starts 60 pixels farther right, and its right edge lines up with the longest label's right edge relative to their respective regions. The longest label itself has no unused horizontal space, so these three choices do not move it.

\strong{Alignment and gap are different:} On a right-facing rectangular or slanted tree with no node symbols, \code{gap_length=6} and \code{align="left"} place text 6 pixels after its tip. In the 100/40 example, \code{align="right"} puts the shorter text 66 pixels after its tip: 6 pixels of gap plus 60 pixels of unused label-region width. The extra 60 pixels come from alignment, not a larger gap. To bring the beginning of right-facing labels close to tips, use \code{align="left"} and adjust \code{gap_length}. Changing the font changes measured widths, so the extra alignment space may change too. On the left side of an arch, the region lies to the left of its tip; right-aligned text is therefore the nearer text edge. At the arch top, the region is placed above the tip. Label backgrounds follow the actual text box, not the whole equal-width region.

\strong{Circular exception:} In layouts 3 and 4, labels run along the tip's radial direction and flip on the left half to remain upright. They use their own text width rather than equal-width horizontal regions, so \code{align} has no effect. \code{gap_length} still controls radial clearance beyond the node-symbol edge. Layout 3 follows real tips; layout 4 follows extended tips.

\strong{Slanted directions:} \code{layout(code=7, direction="left")} places readable horizontal labels to the left of the tips; right alignment brings the text's end closer to its tip. With \code{direction="down"}, labels read top-to-bottom; with \code{direction="up"}, labels read bottom-to-top. The equal-length region rotates with the labels, and left/center/right keep the same longest-label reference along the reading axis. Gap is measured in the outward growth direction rather than always to the screen's right. Changing direction never reverses the characters of a label.

Invalid parameters raise a catchable R error with the received value, requirement and corrected example. Interactive sessions with a running JVM also receive a nonmodal explanation dialog; batch sessions, \code{YYTREE_EXAMPLES_NO_GUI=1}, and sessions without a running JVM do not open dialogs.

Showing labels alone does not assign background colors.)",
  'p <- yytree("((A:1,B:2):1,C:3);") +\n  leaf_labels(align = "left", font_size = 14)\nhidden <- p + leaf_labels(show = FALSE)')

document("leaf_label_render", "Color the backgrounds of selected leaf labels.",
  c(df = paste(r"(Required nonempty data frame with \code{name} (known leaf names) and \code{color} (label background colors). Names must be unique. \code{NA} color removes the background, not the text.)", color_rule)),
  paste(r"(Add \code{leaf_labels(show = TRUE)} before this layer. Adding it with labels absent or currently hidden raises an error. The colors fill the label background; they do not change glyph or branch colors. Leaves omitted from \code{df} retain their existing background; a later layer replaces colors for the leaves it selects.)", selector_rule),
  'p <- yytree("((A:1,B:2):1,C:3);") + leaf_labels() +\n  leaf_label_render(data.frame(name = c("A", "C"),\n                               color = c("gold", "#66CCFF80")))')

document("leaf_render", "Set symbols and incoming-branch colors for all or selected leaves.",
  c(size = node_size_arg, fill = node_fill_arg,
    df = r"(Default \code{NULL} applies supplied \code{size} and \code{fill} to every leaf. Otherwise, a nonempty data frame with unique known leaf \code{name} values and optional \code{size} and \code{fill} columns. Each supplied row field overrides the corresponding function argument.)"),
  paste(r"(Only explicitly supplied style fields are changed. A later style layer overrides those fields for its selected leaves. Omitted leaves retain prior styles. \code{leaf_render()} with no arguments has no visible effect.)", selector_rule),
  'p <- yytree("((A:1,B:2):1,C:3);") + leaf_render(size = 3, fill = "blue")\nq <- p + leaf_render(size = 4,\n  df = data.frame(name = c("A", "C"), fill = c("red", "gold")))')

document("inner_node_render", "Set symbols and incoming-branch colors for internal nodes, including the root.",
  c(size = node_size_arg, fill = node_fill_arg,
    df = r"(Default \code{NULL}. Without selectors, apply supplied styles to all internal nodes. With \code{name1} and \code{name2}, style their MRCA only. Otherwise supply a nonempty data frame with \code{name1} and \code{name2}, and optional \code{size} and \code{fill} columns. Rows select MRCAs and override function-level style values.)",
    name1 = paste(mrca_arg, "Without df, leaving both selectors NULL styles all internal nodes."),
    name2 = paste(mrca_arg, "Without df, leaving both selectors NULL styles all internal nodes.")),
  paste(r"(Without \code{df}, supply both leaf selectors or neither. With \code{df}, row selectors take precedence over function-level selectors; there is no mutual-exclusion error. Later layers override only their supplied style fields. The root has no incoming branch.)", selector_rule),
  'base <- yytree("((A:1,B:2):1,C:3);")\nall_nodes <- base + inner_node_render(size = 2, fill = "grey")\none_node <- base + inner_node_render(size = 5, fill = "red",\n                                     name1 = "A", name2 = "B")\nbatch <- base + inner_node_render(size = 3,\n  df = data.frame(name1 = "A", name2 = "B", fill = "orange"))')

annotation_position_arg <- r"(One of \code{"left"}, \code{"right"}, \code{"top"}, \code{"bottom"}, \code{"topleft"}, \code{"topright"}, \code{"bottomleft"}, or \code{"bottomright"}, in fixed screen coordinates. Text remains horizontal in every layout. Left/right text is vertically centered; top/bottom text is horizontally centered, using actual glyph bounds and a 5-logical-pixel edge gap. At corners, opposite text and reference corners face each other with a 5-pixel gap on each axis: for example, topleft puts the text's bottom-right corner above and left of the reference's top-left corner. A supplied \code{df$text_position} overrides the setting per row.)"
document("inner2leaf_highlight", "Shade the clade descending from an MRCA.",
  c(name1 = mrca_arg, name2 = mrca_arg,
    fill = paste(r"(Clade background color, default \code{"green"}. \code{NA} disables the fill. Display alpha is capped at 40/255 so branches remain readable; more transparent input stays transparent.)", color_rule),
    df = r"(Default \code{NULL}: select one clade with both leaf-name arguments. Otherwise a nonempty data frame with \code{name1} and \code{name2}, plus optional \code{fill}, \code{text} and \code{text_position}. Row values override function-level values; duplicate selector pairs are rejected.)",
    text = r"(One nonmissing string, default \code{NULL} for no label. A supplied \code{df$text} overrides it per row. An empty string omits the label.)",
    text_position = paste(r"(Default \code{"left"}.)", annotation_position_arg)),
  paste(r"(The selected ancestor and all descendant leaves define the shaded region. Geometry follows the current rectangular, circular, arch or slanted layout. Highlights are drawn behind branches and do not recolor nodes or branches. Optional black text is drawn after the tree, even when the fill is transparent or NA. All eight text positions use the selected internal node's center as the reference point, independent of node-symbol radius, tree direction, or shaded-region bounds. Multiple layers add highlight regions; overlapping fills are composited in addition order.)", selector_rule),
  'p <- yytree("((A:1,B:2):1,C:3);") +\n  inner2leaf_highlight("A", "B", fill = "#33AA3366")\nq <- yytree("((A:1,B:2):1,C:3);") +\n  inner2leaf_highlight(df = data.frame(name1 = "A", name2 = "B",\n                                      fill = "skyblue"))')

document("inner_node_annotation", "Place a shape and text beside a selected internal node.",
  c(name1 = mrca_arg, name2 = mrca_arg,
    text = r"(Annotation text, default \code{NULL}. Supply one nonmissing string, or a nonmissing \code{text} column in \code{df}. A row's text overrides the function-level string.)",
    shape = r"(One of \code{"rect"} (default), \code{"ellipse"}, or \code{"triangle"}. A supplied \code{df$shape} overrides it per row.)",
    size = r"(Finite nonnegative annotation-box width in logical pixels, default 20. Height is the same as width; this is not a node radius. A supplied \code{df$size} overrides it per row.)",
    fill = paste(r"(Shape fill color, default \code{"pink"}; \code{NA} leaves the shape unfilled. A supplied \code{df$fill} overrides it per row.)", color_rule),
    df = r"(Default \code{NULL}: annotate one MRCA using the two leaf-name arguments. Otherwise a nonempty data frame with \code{name1}, \code{name2} and any of \code{text}, \code{shape}, \code{size}, \code{fill}, \code{text_position}. Omitted style columns use function-level values. Text must be provided at one of these levels.)",
    text_position = paste(r"(Default \code{"right"}.)", annotation_position_arg)),
  paste(r"(The shape is centered on the MRCA. Black text is positioned outside the shape's screen bounds. The shape has a fill but no separate outline parameter. An empty string omits text while keeping the shape. Row selectors override function-level selectors when \code{df} is present. Multiple annotation layers append; they do not replace earlier annotations.)", selector_rule),
  'base <- yytree("((A:1,B:2):1,C:3);")\np <- base + inner_node_annotation("A", "B", text = "AB ancestor")\nq <- base + inner_node_annotation(shape = "ellipse", size = 16,\n  df = data.frame(name1 = "A", name2 = "B", text = "AB", fill = "gold"))')

document("config_yytree", "Start or reuse the Java subprocess used by all yytree previews.",
  c(path2egps = r"(Required. One existing eGPS installation-directory string containing \file{dependency-egps} JARs. The installation's \file{jre} is preferred; if absent, \env{JAVA_HOME} is used. Java 25 or newer is required. The package supplies its bridge JAR, not the eGPS core.)",
    max_memory_heap = r"(One Java heap-limit string, default \code{"4g"}, passed as \code{-Xmx}. Positive integer followed by \code{m} or \code{g}, case-insensitive; for example \code{"512m"}. Changing a configured value requires \code{restart = TRUE}.)",
    max_memory_stack = r"(One per-thread Java stack-size string, default \code{"8m"}, passed as \code{-Xss}. Positive integer followed by \code{k}, \code{m}, or \code{g}, case-insensitive; for example \code{"512k"} or \code{"16m"}. Changing a configured value requires \code{restart = TRUE}.)",
    restart = r"(One nonmissing logical value, default \code{FALSE}. With \code{FALSE}, identical configuration reuses a live subprocess and changed configuration raises an error. \code{TRUE} closes all existing windows and restarts Java with the supplied configuration, even if unchanged.)"),
  r"(Call this function before printing a plot. Loading the package and constructing plot values do not start Java. The first call starts one independent subprocess; all previews in this R session share it. No embedded rJava JVM or eGPS desktop main frame is used, and R's \env{JAVA_HOME} is not modified.

Heap and stack are separate resources: increasing heap does not fix stack overflow in a deeply nested tree. Both memory arguments are checked before launch. Startup failures report Java diagnostics; the returned log path helps diagnose later failures.

After an unexpected Java exit, the next print attempts to start a fresh subprocess from the last successful configuration and warns that old window state was lost. Communication failures, timeouts and user interrupts terminate the subprocess without replaying the in-flight request. Normal startup and requests have a 30-second timeout. Explicit quit clears the configuration and disables automatic recovery until configured again. R exit or package unload attempts to shut down Java. Logs are written below \file{tempdir()/yytree/logs}; the returned configuration gives the actual log path.)",
  r"(\dontrun{
# Set EGPS_HOME to an existing eGPS installation directory first.
egps_home <- Sys.getenv("EGPS_HOME")
stopifnot(nzchar(egps_home), dir.exists(egps_home))
runtime <- config_yytree(egps_home)
p <- yytree("(A:1,B:2);") + leaf_labels()
print(p)
# Changing memory settings closes all existing previews.
runtime <- config_yytree(egps_home, max_memory_heap = "8g",
                         max_memory_stack = "16m", restart = TRUE)
quit_yytree()
})",
  r"(Invisibly returns the effective configuration list: normalized installation root, Java directory, heap/stack settings, runtime probe information, process ID (\code{pid}) and stderr log path (\code{log}).)")

document("quit_yytree", "Close all yytree windows, stop Java, and clear the saved runtime configuration.",
  NULL,
  r"(This function has no parameters. Calling it when Java is not running is safe, including repeated calls. Plot specifications already stored in R remain unchanged. After quitting, call \code{config_yytree()} again before printing; explicit quit does not trigger automatic recovery.

Closing a preview window only disposes that window and keeps Java available. \code{quit_yytree()} instead terminates the subprocess and all of its windows.)",
  r"(\dontrun{
egps_home <- Sys.getenv("EGPS_HOME")
stopifnot(nzchar(egps_home), dir.exists(egps_home))
config_yytree(egps_home)
p <- yytree("(A:1,B:2);")
print(p)
quit_yytree()
quit_yytree()  # Safe even after the subprocess has stopped.
config_yytree(egps_home)  # Required before printing again.
print(p)
quit_yytree()
})", "Invisibly returns \\code{NULL}.")

legend_arg <- r"(One legend-position string: \code{"top_right"} (default), \code{"top_left"}, \code{"bottom_right"}, \code{"bottom_left"}, or \code{"none"}.)"
track_width_arg <- "One finite positive number: width of each annotation column in logical pixels, default 16. This is not the entire track group's width or a relative canvas fraction."
rotation_arg <- "One finite number from -90 to 90 degrees, default 60. Zero is horizontal and positive angles rotate clockwise. Each annotation call controls its own column labels. Rectangular and slanted layouts show these labels; circular and arch layouts do not. Horizontal or shallow labels may overlap when columns are narrow."
document("leaf_annotation_rect", "Append categorical annotation columns matched to leaf names.",
  c(df = r"(Required data frame with a \code{name} column and at least one category column. Names must be nonmissing, unique known leaves. Each remaining column produces one track; \code{NA} categories and omitted leaves are left blank.)",
    legend = legend_arg,
    colors = paste(r"(Default \code{NULL} uses \code{hcl.colors(..., "Dark 3")} per column. Otherwise a category-named color vector shared by columns, or a list named by column containing category-named vectors. Every category must be named, including unused factor levels. \code{NA} mapped colors leave cells blank.)", color_rule),
    width = track_width_arg, text_rotation = rotation_arg,
    title = r"(Default \code{NULL} uses the original column names as legend headings. A character vector named by annotation column changes those legend headings; unspecified columns keep their original names. Names must be unique and known. For a single category column, one unnamed string is also accepted. Track labels and color mappings retain the original column names.)"),
  r"(Factor levels determine category and legend order, including unused levels; other columns use first-occurrence order among nonmissing values. Rows may be in any order. Layers append outward in addition order, with labels between tree and tracks. Track geometry follows the chosen tree layout. Each annotation call has its own legend block, with bold column headings and category entries. Blocks sharing a corner follow addition order, separated by gray dashed lines, and wrap into columns when needed. Repeated annotations retain separate legends. Use leaf_annotation_gap() between calls to set their spacing.)",
  'categories <- data.frame(name = c("C", "A"),\n                         group = factor(c("treated", "control")))\np <- yytree("((A:1,B:2):1,C:3);") + leaf_labels() +\n  leaf_annotation_rect(categories,\n    colors = c(control = "steelblue", treated = "orange"), width = 12)')

document("leaf_annotation_heatmap", "Append numeric heatmap columns matched to leaf names.",
  c(x = r"(Required nonempty numeric matrix with unique known leaf names in \code{rownames(x)}. Infinite entries are rejected; \code{NA} entries are blank. Column names become track titles; absent column names are replaced by \code{V1}, \code{V2}, and so on. Omitted leaves are blank.)",
    legend = legend_arg, colors = heat_colors_arg, limits = limits_arg, width = track_width_arg,
    text_rotation = rotation_arg,
    title = r"(Default \code{NULL} uses the joined column names as the legend heading. One character string replaces that heading without changing the track labels or color scale.)"),
  r"(Rows match leaf names, not row positions. All columns share a single continuous color scale computed from the whole matrix. The legend is a vertical continuous gradient rectangle, 12 by 80 logical pixels, with the maximum at the top and minimum at the bottom. Five numeric ticks use the same scale and palette as the heatmap, including alpha. Equal limits produce a solid midpoint color and a single tick. The title and color bar stay together when legends wrap into columns. Repeated layers append outward rather than replacing previous tracks. Each layer has its own scale and legend; use identical \code{colors} and \code{limits} when layers need a common scale.)",
  'values <- matrix(c(0, 5, NA, 10), nrow = 2,\n                 dimnames = list(c("C", "A"), c("control", "treated")))\np <- yytree("((A:1,B:2):1,C:3);") +\n  leaf_annotation_heatmap(values, limits = c(0, 10), legend = "bottom_right")')

document("leaf_annotation_gap", "Set the spacing between two adjacent leaf annotation groups.",
  c(width = "One finite nonnegative number in logical pixels, default 12. Zero makes the two annotation groups touch; fractional widths are supported."),
  r"(Insert this layer between leaf_annotation_rect() or leaf_annotation_heatmap() calls. It replaces the default 4-pixel spacing only at that boundary, without changing spacing between columns in an annotation. Other layer types do not consume the gap. Consecutive gap layers use the last width. Gaps before the first annotation or after the last annotation have no effect. All seven tree layouts are supported, including every slanted direction. This layer has no legend and does not start Java.)",
  'categories <- data.frame(name = c("A", "B"), group = c("one", "two"))\nvalues <- matrix(c(1, 2), dimnames = list(c("A", "B"), "score"))\np <- yytree("(A:1,B:1);") + leaf_annotation_rect(categories) +\n  leaf_annotation_gap(width = 12) + leaf_annotation_heatmap(values)')

document("free_text", "Place text at a freely chosen canvas coordinate.",
  c(x = "Required finite numeric horizontal anchor coordinate, in the unit selected by mode.",
    y = "Required finite numeric text-baseline coordinate, in the unit selected by mode.",
    text = "Required single nonmissing character string.", mode = mode_arg,
    locator = r"(One string: \code{"left"} (default) starts the text to the right of \code{x}; \code{"right"} ends it to the left; \code{"center"} centers it on \code{x}. The gap applies only to left and right anchors.)",
    gap = "One finite numeric horizontal gap in logical pixels, default 10. May be negative; ignored for a centered anchor.",
    color = paste(r"(Text color, default \code{"black"}. \code{NA} hides the text.)", color_rule),
    size = "One finite positive font size in logical pixels, default 12; independent of mode."),
  paste(r"(The y coordinate is the text baseline, not the top of a text box. The font family follows the plot's label font; this function has no separate font-family argument.)", coordinate_rule),
  'p <- yytree("(A:1,B:2);") +\n  free_text(50, 8, "Tree title", mode = "percent", locator = "center", size = 18)')

for (name in c("free_ellipse", "free_rectangle")) {
  ellipse <- name == "free_ellipse"
  anchor <- if (ellipse) "center" else "upper-left corner"
  document(name, paste("Draw a", if (ellipse) "centered ellipse" else "rectangle anchored at its upper-left corner", "on the canvas."),
    c(x = paste("Required finite numeric horizontal coordinate of the", anchor, "in the selected mode."),
      y = paste("Required finite numeric vertical coordinate of the", anchor, "in the selected mode."),
      width = "Required finite positive full width (not radius), in the unit selected by mode.",
      height = "Required finite positive full height (not radius), in the unit selected by mode.",
      mode = mode_arg, fill_color = fill_arg, stroke_args),
    paste(if (ellipse) "The bounding box extends half the width and height on each side of (x, y)." else "The bounding box extends rightwards and downwards from (x, y).",
      "Fill and outline are independent; disabling both makes the shape invisible.", coordinate_rule),
    if (ellipse) 'p <- yytree("(A:1,B:2);") +\n  free_ellipse(.8, .85, .12, .08, fill_color = "gold", line_type = "dashed")'
    else 'p <- yytree("(A:1,B:2);") +\n  free_rectangle(.74, .81, .12, .08, fill_color = "#FFD70080")')
}

for (name in c("free_line", "free_arrow")) {
  document(name, if (name == "free_line") "Draw a straight line segment on the canvas." else "Draw a straight line with an arrowhead at its end point.",
    c(x1 = "Required finite numeric start x coordinate, in the selected mode.",
      y1 = "Required finite numeric start y coordinate, in the selected mode.",
      x2 = "Required finite numeric end x coordinate, in the selected mode.",
      y2 = "Required finite numeric end y coordinate, in the selected mode.",
      mode = mode_arg, stroke_args,
      if (name == "free_arrow") c(head_style = "One of open (default, outlined V) or filled (solid triangle). Dashes apply only to the shaft.",
        head_length = "NULL (automatic) or one finite positive arrowhead length in logical pixels. Automatic length is max(9, 4 * line_thickness).",
        head_width = "NULL (automatic) or one finite positive total arrowhead width in logical pixels. Automatic width is max(7, 2.5 * line_thickness).")),
    paste(if (name == "free_arrow") "The arrowhead is at (x2, y2); reversing the end points reverses the arrow. Head dimensions are independent of mode, and shrink together when the segment is too short. A zero-length arrow is not drawn."
      else "The line has no fill or arrowhead; its end points determine its direction and length.", coordinate_rule),
    paste0('p <- yytree("(A:1,B:2);") +\n  ', name, '(.65, .9, .8, .9, line_color = "red", line_thickness = 2)'))
}

document("free_polygon", "Draw a closed polygon through canvas-coordinate vertices.",
  c(points = "Required finite numeric matrix with two columns (x, y) and at least three rows. Rows are successive vertices, in the unit selected by mode.",
    mode = mode_arg, fill_color = fill_arg, stroke_args),
  paste("The last vertex is automatically connected to the first; repeating the first row is unnecessary. Fill and outline can be disabled independently.", coordinate_rule),
  'vertices <- rbind(c(.7, .8), c(.8, .9), c(.6, .9))\np <- yytree("(A:1,B:2);") + free_polygon(vertices, fill_color = "gold")')

document("free_bezier", "Draw one cubic Bezier curve on the canvas.",
  c(points = "Required finite numeric matrix with exactly four rows and two columns (x, y). Rows are start point, first control point, second control point, and end point, in the selected mode.",
    mode = mode_arg, stroke_args),
  paste("Only the start and end points are necessarily on the curve. The middle two points control tangent direction and curvature. The curve is open and has no fill parameter.", coordinate_rule),
  'control <- rbind(c(.1, .85), c(.3, .65), c(.6, .95), c(.8, .8))\np <- yytree("(A:1,B:2);") + free_bezier(control, line_color = "purple")')

document("free_bezier_ring", "Draw a filled ribbon bounded by two cubic Bezier curves.",
  c(points1 = "Required finite numeric four-row, two-column (x, y) matrix: start, control 1, control 2 and end of the first boundary.",
    points2 = "Required matrix with the same format for the second boundary, ordered in the same start-to-end direction as points1 (do not reverse its rows).",
    mode = mode_arg,
    fill_color = paste(r"(Ribbon fill color, default \code{"grey"}; \code{NA} disables filling.)", color_rule),
    line_color = paste(r"(Boundary stroke color, default \code{NA} (no outline). Supply a color to draw the entire closed boundary.)", color_rule),
    line_thickness = stroke_args[["line_thickness"]], line_type = stroke_args[["line_type"]]),
  paste("The first boundary is traversed forwards, connected to the end of the second boundary, then the second boundary is traversed backwards and connected to the first start point. The result is a closed ribbon, not a circular ring primitive.", coordinate_rule),
  'upper <- rbind(c(.1, .8), c(.3, .6), c(.6, .9), c(.8, .75))\nlower <- upper\nlower[, 2] <- lower[, 2] + .05\np <- yytree("(A:1,B:2);") +\n  free_bezier_ring(upper, lower, fill_color = "#9370DB80")')

document("free_heatmap", "Place a numeric matrix in a rectangular region of the canvas.",
  c(x = "Required finite numeric horizontal coordinate of the heatmap's upper-left corner, in the selected mode.",
    y = "Required finite numeric vertical coordinate of the heatmap's upper-left corner, in the selected mode.",
    width = "Required finite positive total width of the heatmap, in the selected mode; not an individual cell width.",
    height = "Required finite positive total height of the heatmap, in the selected mode; not an individual cell height.",
    matrix = r"(Required nonempty numeric matrix. Infinite values are rejected; \code{NA} entries are left blank. Row and column names are ignored; no leaf-name matching is performed.)",
    mode = mode_arg, colors = heat_colors_arg, limits = limits_arg),
  paste("Rows run from top to bottom and columns from left to right. Cells divide the requested rectangle evenly. No labels, borders or legend are added automatically. Use free_text for independent labels or leaf_annotation_heatmap for tracks linked to leaves.", coordinate_rule),
  'values <- matrix(c(0, 2, NA, 6, 8, 10), nrow = 2)\np <- yytree("(A:1,B:2);") +\n  free_heatmap(.65, .8, .25, .12, values, limits = c(0, 10))')

document("print.yytree", "Display a yytree specification in an independent interactive Swing preview.",
  c(x = "Required yytree plot specification.",
    "..." = "Reserved for the generic print interface; currently unused.",
    new_window = r"(One nonmissing logical value. Default \code{FALSE} replaces the current preview; \code{TRUE} opens another independent preview. All previews share the configured Java subprocess.)"),
  r"(\subsection{Displaying and reusing plots}{
Call \code{config_yytree()} first. Explicit \code{print(p)} and automatic printing of \code{p} at the R prompt both display a preview. The R object and original tree remain unchanged by window interaction. Closing a window does not stop Java; use \code{quit_yytree()} to stop all previews and the subprocess.
}
\subsection{Fit, zoom and pan}{
Initially the full logical canvas fits inside the window without enlarging small plots. Window resizing follows automatic fit until manual zoom. The mouse wheel zooms around the pointer; Width and Height percentage controls are linked and zoom around the visible center. They accept decimals, Enter or focus-loss commits, and one-percentage-point steps. Invalid input retains the last valid view; allowed bounds appear in tooltips.

After manual zoom, resizing preserves its scale; scrollbars appear only on overflow. Drag the canvas with the left mouse button or use scrollbars to pan within canvas bounds. Fit frame clears panning and returns to automatic fit while retaining hover switches. The first fitted display is the fixed 100 percent baseline for both controls and the bottom scale readout. Resizing and Fit frame do not change that baseline, so fitting after a resize may not read 100 percent. Entering 100 restores the first fitted scale. Printing again resets fit and establishes a new baseline. Controls wrap in narrow windows. GUI zoom does not change logical canvas or export dimensions.
}
\subsection{Node inspection}{
Show Crosshair and Snap To Point default to on in each independent preview. Within 12 interface pixels, a node glows and the crosshair snaps to it. Tooltips show identity, node type, incoming branch length and logical position; the root has no incoming branch. Mouse and Snap status fields distinguish pointer and node coordinates. Turning either switch off retains node inspection. Dragging hides hover overlays; exports omit them. Switches reset when printing again. Hover reuses background-rendered viewport images.

Add free shapes opens a modeless editor for all nine free drawing functions. Select tools on the left, then create or edit objects on the canvas. Curves start as draggable templates; polygons finish with double-click or Enter. Object handles change geometry and the Properties panel changes styles. New objects use percent coordinates; changing mode preserves placement. While editing, middle drag or Space plus left drag pans, the wheel zooms, Escape cancels, Delete removes, and Ctrl+Z / Ctrl+Y undo and redo. Closing the editor keeps preview changes; printing again resets them. Copy code generates the complete free drawing block: replace the old block after your base plot object rather than appending duplicates. Changes do not update R objects or exported images until you copy and execute the code. Heatmaps accept pasted numeric matrices and retain their source data for code generation; older heatmaps without source data require the original matrix before code can be copied.
})",
  r"(\dontrun{
egps_home <- Sys.getenv("EGPS_HOME")
stopifnot(nzchar(egps_home), dir.exists(egps_home))
config_yytree(egps_home)
p <- yytree("((A:1,B:2):1,C:3);") + leaf_labels()
print(p)
print(p + yytree::layout(name = "circ"))  # Replace the current preview.
print(p, new_window = TRUE)              # Open an additional preview.
quit_yytree()
})", "Invisibly returns the unchanged plot \\code{x}.",
  r"(\method{print}{yytree}(x, ..., new_window = FALSE))")

document("+.yytree", "Append one layer to a plot specification without modifying the original plot.",
  c(e1 = "Required yytree plot specification on the left-hand side.",
    e2 = "Required yytree_layer value on the right-hand side. Other objects, including another complete plot, are rejected."),
  r"(\subsection{Layer order}{
Use \code{plot + layer + layer}. This constructs an R value without starting Java; display it with \code{print()}. Layout and label-setting layers use the last settings. Node styles override only fields they explicitly supply. Track, highlight, free-element and internal-node annotation layers append; they do not replace earlier layers. Rendering uses fixed categories (for example highlights behind branches), so addition order is not a global z-order across all layer types.
}
\subsection{Dependencies and selectors}{
Add \code{leaf_labels(show = TRUE)} before \code{leaf_label_render()}; the most recent label setting must be visible at the point that layer is added. Unknown leaves, duplicate selector rows and invalid MRCA pairs are rejected during composition. In data-frame annotation layers, row fields override matching function-level fields; missing columns inherit the layer's function-level values. A missing cell is not the same as an omitted column. Consult each function for its supported columns and \code{NA} behavior.
})",
  'original <- yytree("((A:1,B:2):1,C:3);")\ncomposed <- original + leaf_labels() +\n  leaf_label_render(data.frame(name = "A", color = "gold")) +\n  yytree::layout(name = "circ")\nstopifnot(length(original$layers) == 0, length(composed$layers) == 3)',
  "A new \\code{yytree} value with the layer appended; \\code{e1} remains unchanged.",
  r"(\method{+}{yytree}(e1, e2))")

# Each topic owns exactly its listed functions; links supply broader context.
topics <- list(
  yytree = list(title = "Create a tree plot specification", functions = "yytree", links = c("layout", "leaf_labels", "+.yytree", "print.yytree", "yytree-package")),
  layout = list(title = "Choose a tree layout", functions = "layout", links = c("yytree", "leaf_labels", "+.yytree")),
  leaf_labels = list(title = "Leaf label visibility, fonts and background colors", functions = c("leaf_labels", "leaf_label_render"),
    comparison = "leaf_labels controls whether labels exist and how they are positioned and typeset. leaf_label_render then colors selected label backgrounds. It requires a preceding visible-label layer. Use leaf_render to style leaf symbols and branches instead.", links = c("layout", "leaf_render", "+.yytree")),
  node_render = list(title = "Style leaf and internal-node symbols and branches", functions = c("leaf_render", "inner_node_render"),
    comparison = "Both functions set node radius and incoming-branch color. leaf_render selects tips by name; inner_node_render selects internal nodes by an MRCA leaf pair, or all internal nodes when selectors are absent. Neither function adds annotation text or a clade background.", links = c("leaf_label_render", "inner_node_annotation", "inner2leaf_highlight")),
  inner2leaf_highlight = list(title = "Highlight a descendant clade", functions = "inner2leaf_highlight", links = c("inner_node_render", "inner_node_annotation")),
  inner_node_annotation = list(title = "Annotate an internal node with a shape and text", functions = "inner_node_annotation", links = c("inner_node_render", "inner2leaf_highlight", "free_text")),
  runtime = list(title = "Configure and stop the yytree Java runtime", functions = c("config_yytree", "quit_yytree"),
    comparison = "config_yytree starts or reuses one shared Java subprocess; quit_yytree stops it and clears configuration. Closing a window only closes that preview. To change heap, stack or installation settings, call config_yytree with restart = TRUE; to resume after quit_yytree, configure again explicitly.", links = c("print.yytree", "yytree")),
  leaf_annotation = list(title = "Categorical and numeric leaf annotation tracks", functions = c("leaf_annotation_rect", "leaf_annotation_heatmap"),
    comparison = "Both functions match by leaf name and append one track per data column. leaf_annotation_rect uses a data-frame name column and category-to-color mappings. leaf_annotation_heatmap uses matrix row names and one continuous numeric scale. Width is per column in logical pixels. Missing leaves or values are blank. Legends are grouped by annotation call, with headings and dashed separators. Use leaf_annotation_gap between calls to set their spacing, or free_heatmap for a matrix placed independently of the tree.", links = c("leaf_annotation_gap", "layout", "leaf_labels", "free_heatmap")),
  leaf_annotation_gap = list(title = "Spacing between leaf annotation groups", functions = "leaf_annotation_gap", links = c("leaf_annotation_rect", "leaf_annotation_heatmap", "leaf_labels")),
  free_text = list(title = "Place free text on the canvas", functions = "free_text", links = c("free_ellipse", "inner_node_annotation", "yytree")),
  free_shapes = list(title = "Draw free ellipses and rectangles", functions = c("free_ellipse", "free_rectangle"),
    comparison = "Both functions use full width and height and share fill and stroke settings. Their anchors differ: ellipse coordinates identify the center; rectangle coordinates identify the upper-left corner. For an identical bounding box, subtract half the width and height from the ellipse center to obtain the rectangle anchor.", links = c("free_text", "free_polygon", "yytree")),
  free_lines = list(title = "Draw free lines and arrows", functions = c("free_line", "free_arrow"),
    comparison = "free_line draws a segment; free_arrow adds an open or filled arrowhead at the second end point, with optional independent head_length and head_width. Both use the same stroke controls, logical-pixel stroke width and selected coordinate mode.", links = c("free_bezier", "free_text", "yytree")),
  free_polygon = list(title = "Draw a free polygon", functions = "free_polygon", links = c("free_rectangle", "free_bezier_ring", "yytree")),
  free_bezier = list(title = "Draw cubic Bezier curves and ribbons", functions = c("free_bezier", "free_bezier_ring"),
    comparison = "free_bezier draws one open stroked curve through four control points. free_bezier_ring closes the region between two such boundaries, each ordered in the same direction. The ribbon defaults to grey fill without an outline; the open curve defaults to a black stroke without a fill parameter.", links = c("free_line", "free_polygon", "yytree")),
  free_heatmap = list(title = "Place a free-position numeric heatmap", functions = "free_heatmap", links = c("leaf_annotation_heatmap", "free_text", "yytree")),
  print.yytree = list(title = "Display and interact with a tree preview", functions = "print.yytree", links = c("config_yytree", "quit_yytree", "yytree")),
  plus.yytree = list(title = "Compose a tree plot with layers", functions = "+.yytree", links = c("yytree", "layout", "leaf_labels", "print.yytree")))

namespace <- readLines("rPackage/NAMESPACE", warn = FALSE)
exports <- sub("^export\\((.*)\\)$", "\\1", namespace[startsWith(namespace, "export(")])
methods <- namespace[startsWith(namespace, "S3method(")]
methods <- vapply(strsplit(gsub('[" ]', '', sub("^S3method\\((.*)\\)$", "\\1", methods)), ","), paste, "", collapse = ".")
documented <- unlist(lapply(topics, `[[`, "functions"), use.names = FALSE)
if (anyDuplicated(documented) || !setequal(documented, c(exports, methods)) ||
    !setequal(documented, names(function_docs))) stop("Help topics must cover every export and S3 method exactly once.")
known_aliases <- c(documented, "yytree-package")
for (topic in topics) if (!all(topic$links %in% known_aliases)) stop("Unknown related-function link.")

section <- function(tag, content) c(paste0("\\", tag, "{"), content, "}")
function_usage <- function(name) {
  override <- function_docs[[name]]$usage
  if (!is.null(override)) return(override)
  text <- paste(deparse(args(get(name, envir = implementation)), width.cutoff = 60), collapse = "\n")
  text <- gsub(" +\n", "\n", text)
  text <- sub("^function ", name, text)
  sub("[[:space:]]*NULL$", "", text)
}
dir.create("rPackage/man", showWarnings = FALSE)
for (topic_name in names(topics)) {
  topic <- topics[[topic_name]]
  functions <- topic$functions
  docs <- function_docs[functions]
  grouped <- length(functions) > 1L
  argument_names <- unique(unlist(lapply(docs, function(doc) names(doc$arguments))))
  arguments <- vapply(argument_names, function(argument) {
    owners <- functions[vapply(docs, function(doc) argument %in% names(doc$arguments), FALSE)]
    descriptions <- vapply(function_docs[owners], function(doc) doc$arguments[[argument]], "")
    body <- if (length(unique(descriptions)) == 1L) descriptions[1] else
      paste(paste0("\\strong{\\code{", owners, "}:} ", descriptions), collapse = "\n\n")
    if (grouped && length(owners) == 1L) body <- paste0("\\strong{\\code{", owners, "} only:} ", body)
    paste0("\\item{", argument, "}{", body, "}")
  }, "")
  descriptions <- if (grouped) paste0("\\strong{\\code{", functions, "()}:} ", vapply(docs, `[[`, "", "description")) else docs[[1]]$description
  details <- if (grouped) unlist(lapply(functions, function(name)
    c(paste0("\\subsection{", name, "}{"), function_docs[[name]]$details, "}"))) else docs[[1]]$details
  value <- if (grouped) c("\\describe{", vapply(functions, function(name)
    paste0("\\item{\\code{", name, "()}}{", function_docs[[name]]$value, "}"), ""), "}") else docs[[1]]$value
  examples <- unlist(lapply(functions, function(name) c(if (grouped) paste0("# ", name, "()"), function_docs[[name]]$examples, "")))
  output <- c("% Generated by scripts/generate_yytree_help.R; edit that source instead.",
    paste0("\\name{", topic_name, "}"), paste0("\\alias{", functions, "}"),
    section("title", topic$title), section("description", paste(descriptions, collapse = "\n\n")),
    section("usage", vapply(functions, function_usage, "")), section("arguments", arguments),
    section("details", details),
    if (grouped) c("\\section{Similarities and differences}{", topic$comparison, "}"),
    section("value", value),
    section("seealso", paste(paste0("\\code{\\link{", topic$links, "}}"), collapse = ", ")),
    section("examples", examples))
  writeLines(output, file.path("rPackage/man", paste0(topic_name, ".Rd")), useBytes = TRUE)
}

writeLines(c("% Generated by scripts/generate_yytree_help.R; edit that source instead.",
  "\\name{yytree-package}", "\\alias{yytree-package}", "\\docType{package}",
  section("title", "Scriptable interactive eGPS trees"),
  section("description", "Build ordinary R plot specifications by adding layers, then display them in an independent Java Swing preview. Each function or closely related pair has its own help topic."),
  "\\section{Quick start}{",
  "Constructing a plot needs no Java process. To display it, first configure an eGPS installation with Java 25 or newer; see \\code{\\link{config_yytree}}.", "}",
  "\\section{Find a function}{", "\\describe{",
  "\\item{Tree and layout}{\\code{\\link{yytree}}, \\code{\\link{layout}}.}",
  "\\item{Composition and preview}{\\code{\\link{+.yytree}}, \\code{\\link{print.yytree}}.}",
  "\\item{Runtime}{\\code{\\link{config_yytree}}, \\code{\\link{quit_yytree}}.}",
  "\\item{Labels and nodes}{\\code{\\link{leaf_labels}}, \\code{\\link{leaf_label_render}}, \\code{\\link{leaf_render}}, \\code{\\link{inner_node_render}}.}",
  "\\item{Clade annotations}{\\code{\\link{inner2leaf_highlight}}, \\code{\\link{inner_node_annotation}}.}",
  "\\item{Leaf tracks}{\\code{\\link{leaf_annotation_rect}}, \\code{\\link{leaf_annotation_heatmap}}, \\code{\\link{leaf_annotation_gap}}.}",
  "\\item{Free drawing}{\\code{\\link{free_text}}, \\code{\\link{free_ellipse}}, \\code{\\link{free_rectangle}}, \\code{\\link{free_line}}, \\code{\\link{free_arrow}}, \\code{\\link{free_polygon}}, \\code{\\link{free_bezier}}, \\code{\\link{free_bezier_ring}}, \\code{\\link{free_heatmap}}.}",
  "}", "}",
  section("examples", 'p <- yytree("((A:1,B:2):1,C:3);") +\n  yytree::layout(name = "rect_align") + leaf_labels()\nstopifnot(inherits(p, "yytree"))\n\\dontrun{\negps_home <- Sys.getenv("EGPS_HOME")\nstopifnot(nzchar(egps_home), dir.exists(egps_home))\nconfig_yytree(egps_home)\nprint(p)\nquit_yytree()\n}')),
  "rPackage/man/yytree-package.Rd", useBytes = TRUE)
cat("Generated", length(topics) + 1L, "focused help topics.\n")
