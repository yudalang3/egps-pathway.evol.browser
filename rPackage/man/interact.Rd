\name{interact}
\alias{interact}
\title{Attach node hover information}
\usage{interact(df)}
\arguments{\item{df}{A nonempty data frame with a unique character or factor name column. Other columns contain tooltip values.}}
\description{Matches named leaves, internal nodes and roots exactly. Unknown or ambiguous names are errors. The last interact layer replaces earlier data.}
\value{A yytree layer added using +.}
\details{Column order is retained. Missing values display as an em dash. Text, numeric, logical, factor and date columns are supported.}
\examples{
p <- yytree("((Human:1,Mouse:1)N1:1,Chicken:2)Root;")
p <- p + interact(data.frame(name = c("Human", "N1", "Root"),
                            description = c("Sample", "Ancestor", "Root")))
}
