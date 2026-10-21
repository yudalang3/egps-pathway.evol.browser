args <- commandArgs(trailingOnly = TRUE)
root <- if (length(args)) normalizePath(args[1]) else normalizePath('.')
dest <- file.path(root, 'src/module/evolview/moderntreeviewer/examples')
dir.create(dest, recursive = TRUE, showWarnings = FALSE)
library(ape)
stopifnot(as.character(packageVersion('ape')) == '5.8.1')
write_example <- function(name, text) writeLines(text, file.path(dest, name), useBytes = TRUE)
write_example('01-great-apes.nwk', '(((Human:6.5,(Chimpanzee:1.5,Bonobo:1.5):5):2,Gorilla:8.5):4,Orangutan:12.5);')
primates <- readLines(file.path(root, 'src/module/evolview/phylotree/visualization/primary/swing/test.tree.txt'))[2]
write_example('02-primate-support.nwk', paste0(trimws(primates), ';'))
write_example('03-mammals.nwk', readLines(file.path(root, 'docs/spiral.layout.tree.nwk')))
data(bird.orders)
data(hivtree.newick)
stopifnot(Ntip(bird.orders) == 23, Ntip(read.tree(text = hivtree.newick)) == 193)
write_example('04-bird-orders.nwk', write.tree(bird.orders, digits = 15))
write_example('05-hiv-sequences.nwk', hivtree.newick)
# Preserve source files separately from generated, bundled Newick resources.
originals <- file.path(root, 'orig_matrials/modern-tree-examples')
dir.create(originals, recursive = TRUE, showWarnings = FALSE)
for (name in c('bird.orders.rda', 'hivtree.newick.rda'))
    file.copy(system.file('data', name, package = 'ape'), originals, overwrite = FALSE)
file.copy(system.file('DESCRIPTION', package = 'ape'), originals, overwrite = FALSE)
file.copy(file.path(R.home('share'), 'licenses/GPL-2'), file.path(dest, 'APE-DATA-GPL-2.txt'), overwrite = TRUE)
# Deterministic topology, labels and positive branch lengths; no runtime generation.
balanced <- function(ids, named = FALSE, depth = 0L) {
    if (length(ids) == 1) return(sprintf('Sample%03d:%.3f', ids, 0.1 + (ids %% 7) / 20))
    middle <- length(ids) %/% 2
    label <- if (named) paste0('Clade', min(ids), 'to', max(ids)) else ''
    paste0('(', balanced(ids[seq_len(middle)], named, depth + 1L), ',',
           balanced(ids[(middle + 1):length(ids)], named, depth + 1L), ')', label, ':',
           sprintf('%.3f', 0.15 + (depth %% 5) / 20))
}
write_example('06-balanced.nwk', paste0(balanced(1:16, TRUE), ';'))
ladder <- 'Sample001:0.08'
for (i in 2:12) ladder <- sprintf('(%s,Sample%03d:%.3f):0.12', ladder, i, 0.1 + (i %% 4) * 0.3)
write_example('07-ladder.nwk', paste0(ladder, ';'))
write_example('08-multifurcation.nwk', '((A:0,B:0.2,C:0.4,D:0.1):0,(E:0.1,F:0.2,G:0.3):0.2,(H:0.4,I:0.2,J:0.1):0.1,K:0.3,L:0.5);')
write_example('09-topology.nwk', '(((A,B),(C,D)),((E,F),(G,H)),(I,J,K,L));')
write_example('10-table.tpv', c('Root\tGroupA:0.3,GroupB:0.5',
    'GroupA\tA1:0.1,A2:0.2,A3:0.3,A4:0.4', 'GroupB\tB1:0.4,B2:0.3,B3:0.2,B4:0.1'))
write_example('10-node-styles.tsv', c('name\tfillColor\tcircleRadius\tlineThickness\tdisplayName',
    'GroupA\t#0072B2\t5\t3\ttrue', 'GroupB\t#D55E00\t5\t3\ttrue',
    'A1\t#0072B2\t3\t2\ttrue', 'B1\t#D55E00\t3\t2\ttrue'))
write_example('11-grouped-64.nwk', paste0(balanced(1:64), ';'))
write_example('12-grouped-256.nwk', paste0(balanced(1:256), ';'))
# Round trips must preserve the real datasets rather than introduce fabricated biology.
bird_export <- read.tree(file.path(dest, '04-bird-orders.nwk'))
hiv_export <- read.tree(file.path(dest, '05-hiv-sequences.nwk'))
stopifnot(isTRUE(all.equal(bird.orders, bird_export)),
          isTRUE(all.equal(read.tree(text = hivtree.newick), hiv_export)))
cat('Generated 12 examples; verified bird and HIV source round trips.\n')
