for (file in list.files("rPackage/R", full.names = TRUE)) source(file)
exports <- readLines("rPackage/NAMESPACE")
exports <- sub("export\\((.*)\\)","\\1",exports[startsWith(exports,"export(")])
descriptions <- c(tree="A hclust or phylo object, one Newick string, or a Newick file path.",
 path2egps="The eGPS installation directory containing dependency-egps and its Java runtime.",
 max_memory_heap="Maximum Java heap (-Xmx), for example 4g. Use restart = TRUE to change a running JVM.",
 max_memory_stack="Per-thread Java stack (-Xss), for example 8m. Use restart = TRUE to change it; increasing heap does not increase the stack.",
 restart="Restart the Java subprocess, closing all its windows. Defaults to FALSE.",
 df="A data frame keyed by name, or by name1 and name2 for MRCA selectors.",
 name1="First leaf name identifying an MRCA.",name2="Second distinct leaf name identifying an MRCA.",
 mode="Coordinate unit: relative (0 to 1), percent (0 to 100), or pixel.",
 legend="Legend corner: top_right, top_left, bottom_right, bottom_left, or none.",
 colors="Heatmap palette colors, or named category colors (optionally a list keyed by column).",
 limits="Two numeric heatmap limits. NULL uses the range of the entire matrix; no standardization is performed.",
 matrix="A numeric matrix for a free-position heatmap; no leaf matching is performed.",
 points="A two-column numeric coordinate matrix. Bezier curves require exactly four rows.",
 points1="Four control points for the first cubic boundary of a ribbon.",
 points2="Four control points for the second boundary, ordered in the same direction.",
 locator="Horizontal text anchor: left, center or right.",gap="Text anchor gap in logical pixels.",
 align="Leaf label column alignment: left, center or right.",
 font_family="Optional Java font family. NULL retains the eGPS preference.",
 font_size="Optional leaf label font size in logical pixels.",
 code="Layout number from 1 to 7.",name="Layout name: rect, rect_align, circ, circ_align, arch, arch_align or slant.",
 show="Whether leaf labels are displayed.",text="Text to display.",
 size="Node radius or annotation box size, in logical pixels; text uses pixel font size.",
 fill="R fill color, including alpha. Node styles also color the incoming branch.",
 fill_color="R fill color; NA disables filling.",line_color="R stroke color; NA disables the stroke.",
 line_thickness="Nonnegative stroke width in logical pixels.",line_type="solid, dashed or dotted.",
 shape="Node annotation shape: rect, ellipse or triangle.",
 x="Horizontal coordinate, or the numeric matrix for leaf_annotation_heatmap.",y="Vertical coordinate.",
 width="Canvas width in pixels, free element width in the chosen unit, or track width in pixels.",
 height="Canvas height in pixels or free element height in the chosen coordinate unit.",
 color="Text color.",x1="Start x coordinate.",y1="Start y coordinate.",x2="End x coordinate.",y2="End y coordinate.")
arguments <- unique(unlist(lapply(exports,function(name)names(formals(get(name))))))
usage <- unlist(lapply(exports,function(name) {
 text <- paste(deparse(args(get(name)), width.cutoff=85),collapse="\n")
 text <- sub("^function ",name,text)
 text <- gsub(" +\n", "\n", text)
 sub(" *NULL$","",text)
}))
items <- vapply(arguments,function(name){description<-descriptions[name];if(is.na(description))description<-"Nonnegative canvas margin in logical pixels.";paste0("\\item{",name,"}{",description,"}")},"")
dir.create("rPackage/man", showWarnings=FALSE)
writeLines(c("\\name{yytree}",paste0("\\alias{",exports,"}"),"\\alias{print.yytree}","\\alias{+.yytree}","\\title{Composable interactive eGPS trees}",
 "\\description{Build an ordinary R plotting specification and display it in an independent Java Swing window using eGPS Modern Tree View.}",
 "\\usage{",usage,"\\method{print}{yytree}(x, ..., new_window = FALSE)","\\method{+}{yytree}(e1, e2)","}","\\arguments{",items,
 "\\item{...}{Reserved for the generic print interface.}",
 "\\item{new_window}{Open a separate preview rather than replacing the current preview.}",
 "\\item{e1}{A yytree plot value.}","\\item{e2}{A yytree layer.}","}",
 "\\details{Call config_yytree before printing. All previews reuse one Java subprocess. Closing windows only disposes the GUI. quit_yytree closes all windows and exits Java; call config_yytree again before printing. Use restart = TRUE to change a running configuration. After unexpected Java exit, the next print restarts it with the last successful configuration; old window state is lost. In-flight communication failures and interrupts terminate Java without replaying the request. R exit or package unload cleans up the subprocess. Loading the package and constructing plot values do not start Java. Seven layouts preserve input branch lengths; aligned layouts only add display extensions. Add leaf_labels(show = TRUE) before leaf_label_render. Label rendering colors the label background. Leaf annotation rows match leaf names, not row positions. Multiple track layers append outwards. Missing annotations are left blank. Colors follow R RGBA conventions. The origin for free elements is the upper-left corner of the entire logical canvas. Ellipses use their centre; rectangles and free heatmaps use their upper-left corner. Canvas dimensions are logical pixels. Previews initially fit the full canvas without enlarging small plots and follow window resizing until the first wheel zoom. Afterwards resizing preserves manual zoom and scrollbars appear only on overflow. Printing again restores automatic fit. GUI zoom does not change the R value or export dimensions. The current preview is reused by default; print(x, new_window = TRUE) opens another independent view. Node size is radius; annotation size is box width.}",
 "\\value{yytree returns a yytree object; layer functions return a yytree_layer. config_yytree invisibly returns effective runtime configuration, including PID and log path. quit_yytree invisibly returns NULL. Printing invisibly returns the unchanged plot.}",
 "\\examples{", "y <- yytree(hclust(dist(USArrests))) + yytree::layout(name = \"slant\") +", "  leaf_labels(show = TRUE)","stopifnot(inherits(y, \"yytree\"))", "}"),"rPackage/man/yytree.Rd")
