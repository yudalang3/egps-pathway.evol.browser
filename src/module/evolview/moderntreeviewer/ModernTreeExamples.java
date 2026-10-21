package module.evolview.moderntreeviewer;

import egps2.EGPSProperties;
import module.evolview.moderntreeviewer.io.ParamsAssignerAndParser4ModernTreeView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Bundled demonstration data; installed copies belong to the user and are never replaced. */
final class ModernTreeExamples {
    record Example(String file, int leaves, String title, String description, String layout,
                   Map<String, String> settings) { }

    static final List<Example> ALL = List.of(
            new Example("01-great-apes.nwk", 5, "Great apes: teaching time tree",
                    "Existing teaching data; ages are illustrative, not a calibrated research result.",
                    "Rectangular phylogram", Map.of("tree.branch.length.unit", "MYA (illustrative)", "tree.need.reverse.axis", "true")),
            new Example("02-primate-support.nwk", 11, "Primates: bootstrap support",
                    "Existing teaching data; inspect support values 65, 75, 95 and 100.",
                    "Rectangular phylogram", Map.of("input.newick.format", "0", "inner.show.bootstrap", "true")),
            new Example("03-mammals.nwk", 31, "Mammals: circular teaching tree",
                    "Existing repository teaching data; branch lengths are not calibrated ages.",
                    "Circular phylogram", Map.of()),
            new Example("04-bird-orders.nwk", 23, "Bird orders: historical research data",
                    "ape bird.orders; Sibley and Ahlquist (1990). Branch lengths measure DNA/DNA hybridization delta T50H.",
                    "Rectangular phylogram", Map.of("tree.branch.length.unit", "Delta T50H")),
            new Example("05-hiv-sequences.nwk", 193, "HIV-1: 193 research sequences",
                    "ape hivtree.newick; Strimmer and Pybus (2001). Original branch lengths retained; no time unit inferred. Enable leaf.show.label to inspect sequence names.",
                    "Radial phylogram", Map.of("leaf.show.label", "false")),
            new Example("06-balanced.nwk", 16, "Balanced binary tree",
                    "Synthetic demonstration; inspect named internal clades.",
                    "Rectangular phylogram", Map.of("inner.show.label", "true")),
            new Example("07-ladder.nwk", 12, "Ladder tree: unequal branches",
                    "Synthetic demonstration; compare short and long branches in an unbalanced tree.",
                    "Rectangular phylogram", Map.of("inner.show.branch.length", "true")),
            new Example("08-multifurcation.nwk", 12, "Multifurcations and zero branches",
                    "Synthetic demonstration; root and internal polytomies include zero-length branches.",
                    "Radial phylogram", Map.of()),
            new Example("09-topology.nwk", 12, "Topology without branch lengths",
                    "Synthetic demonstration; cladogram geometry does not represent evolutionary distances.",
                    "Slant cladogram", Map.of("input.newick.format", "8", "tree.show.axis.bar", "false", "tree.show.scale.bar", "false")),
            new Example("10-table.tpv", 8, "Table tree with node styles",
                    "Synthetic demonstration; named groups have colored branches and different node sizes.",
                    "Rectangular phylogram", Map.of("inner.show.label", "true")),
            new Example("11-grouped-64.nwk", 64, "64-tip circular tree",
                    "Synthetic demonstration; compare circular and radial layouts using the layout controls.",
                    "Circular phylogram", Map.of()),
            new Example("12-grouped-256.nwk", 256, "256-tip spiral tree",
                    "Synthetic demonstration; leaf names are initially hidden. Set leaf.show.label=true to show them.",
                    "Spiral alpha phylogram", Map.of("leaf.show.label", "false", "layout.spiral.extent.degree", "1080")));

    private ModernTreeExamples() { }

    static Path directory() {
        return Path.of(EGPSProperties.PROPERTIES_DIR, "bioData", "example", "modernTreeView");
    }

    static List<String> resources() {
        var files = new java.util.ArrayList<String>();
        for (Example example : ALL) files.add(example.file());
        files.addAll(List.of("10-node-styles.tsv", "SOURCES.txt", "APE-DATA-GPL-2.txt"));
        return List.copyOf(files);
    }

    static void install(Path directory) throws IOException {
        Files.createDirectories(directory);
        for (String name : resources()) {
            Path target = directory.resolve(name);
            if (Files.isRegularFile(target)) continue;
            try (InputStream input = ModernTreeExamples.class.getResourceAsStream("examples/" + name)) {
                if (input == null) throw new IOException("Missing bundled tree example: " + name);
                // Publish only complete files. The target is never opened for writing or replaced.
                Path temporary = Files.createTempFile(directory, ".tree-example-", ".tmp");
                try {
                    Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
                    try { Files.move(temporary, target); }
                    catch (FileAlreadyExistsException existing) {
                        if (!Files.isRegularFile(target)) throw existing;
                    }
                } finally { Files.deleteIfExists(temporary); }
            } catch (IOException error) {
                throw new IOException("Cannot install tree example at " + target + ": " + error.getMessage(), error);
            }
        }
    }

    static String text(int index, Path directory) {
        Example example = ALL.get(index);
        Map<String, String> active = new LinkedHashMap<>();
        active.put("input.newick.text", "");
        active.put("input.newick.file", example.file().endsWith(".tpv") ? "" : directory.resolve(example.file()).toString());
        active.put("input.table.file", example.file().endsWith(".tpv") ? directory.resolve(example.file()).toString() : "");
        active.put("input.newick.format", "1");
        active.put("input.node.styles.file", example.file().endsWith(".tpv") ? directory.resolve("10-node-styles.tsv").toString() : "");
        active.put("layout.initial", example.layout());
        active.put("tree.show.title", "true");
        active.put("tree.title.string", example.title());
        active.put("tree.branch.length.unit", "");
        active.putAll(example.settings());
        var parser = new ParamsAssignerAndParser4ModernTreeView();
        active.forEach((key, value) -> parser.addKeyValueEntryBean(key, value, ""));
        String parameters = parser.getExampleString().lines().map(line -> {
            String key = line.stripLeading();
            int separator = key.indexOf('=');
            return key.startsWith("$") && separator > 0 && !active.containsKey(key.substring(1, separator))
                    ? "# " + line : line;
        }).collect(java.util.stream.Collectors.joining("\n"));
        return "# Example " + (index + 1) + ": " + example.title() + " (" + example.leaves() + " tips)\n"
                + "# " + example.description() + "\n" + sourceNotes(example)
                + "# Full provenance and license: " + directory.resolve("SOURCES.txt") + "\n" + parameters;
    }

    private static String sourceNotes(Example example) {
        return switch (example.file()) {
            case "01-great-apes.nwk" -> "# Source: original VOICE4MTV teaching tree; illustrative ages, not a cited research estimate.\n";
            case "02-primate-support.nwk" -> "# Source: repository src/module/evolview/phylotree/visualization/primary/swing/test.tree.txt, line 2.\n"
                    + "# Teaching data; original topology, branch lengths and support retained; terminating semicolon added.\n";
            case "03-mammals.nwk" -> "# Source: repository docs/spiral.layout.tree.nwk, unchanged.\n"
                    + "# Teaching data without a research citation; lengths are not calibrated ages.\n";
            case "04-bird-orders.nwk" -> "# Source: ape::bird.orders, ape 5.8-1; exported with ape::write.tree(digits=15).\n"
                    + "# Reference: Sibley, C. G. and Ahlquist, J. E. (1990), Phylogeny and Classification of Birds, Yale University Press, figure 353.\n"
                    + "# Branch lengths: Delta T50H (DNA/DNA hybridization); this historical tree is not a statement of current consensus.\n"
                    + apeDataNotes();
            case "05-hiv-sequences.nwk" -> "# Source: ape::hivtree.newick, ape 5.8-1; original Newick string retained.\n"
                    + "# Reference: Strimmer, K. and Pybus, O. G. (2001), Exploring the demographic history of DNA sequences using the generalized skyline plot, Molecular Biology and Evolution 18:2298-2305.\n"
                    + "# Data: 193 HIV-1 group M sequences from the Democratic Republic of Congo; original branch lengths retained without assigning a time unit.\n"
                    + apeDataNotes();
            default -> "# Source: scripts/generate_modern_tree_examples.R; deterministic synthetic demonstration data.\n"
                    + "# Sample labels and branch lengths are illustrative, not measured or inferred biological results.\n";
        };
    }

    private static String apeDataNotes() {
        return "# Documentation: https://emmanuelparadis.r-universe.dev/ape/doc/manual.html\n"
                + "# Upstream: https://github.com/emmanuelparadis/ape\n"
                + "# Data license: GPL-2 (selected from ape's GPL-2 | GPL-3); see APE-DATA-GPL-2.txt. Credit: ape authors and the cited data authors.\n";
    }
}
