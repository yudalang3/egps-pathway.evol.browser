package module.evolview.moderntreeviewer.para;

import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import java.util.*;
import java.util.function.Supplier;

/** A draft belongs to one tree, even if asynchronous import replaces it. */
public final class TreeSettingsSession {
    private final PhylogeneticTreePanel tree;
    private final Supplier<PhylogeneticTreePanel> current;
    private Map<String, Object> baseline;

    public TreeSettingsSession(PhylogeneticTreePanel tree, Supplier<PhylogeneticTreePanel> current, Map<String, Object> defaults) {
        this.tree = tree;
        this.current = current;
        baseline = tree == null ? new LinkedHashMap<>(TreeSettings.defaults()) : TreeSettings.snapshot(tree);
        if (tree == null) baseline.putAll(defaults);
    }
    public boolean canApply() { return tree != null && current.get() == tree; }
    public String unavailableReason() {
        if (tree == null) return "Load a tree to apply parameters. You can still save user defaults.";
        return canApply() ? null : "This tree has been replaced. Close and reopen Advanced parameters.";
    }
    public String text() { return TreeSettings.documentText(baseline); }
    public Map<String, Object> values(String text) {
        Map<String, Object> result = new LinkedHashMap<>(TreeSettings.defaults());
        result.putAll(TreeSettings.parse(text));
        if (Boolean.TRUE.equals(baseline.get("Fonts/Axis and scale bar/Inherit label font"))
                && TreeSettings.changes(baseline, result).keySet().stream().anyMatch(p -> p.startsWith("Fonts/Axis and scale bar/") && !p.endsWith("Inherit label font")))
            result.put("Fonts/Axis and scale bar/Inherit label font", false);
        return result;
    }
    public void apply(String text) {
        if (!canApply()) throw new IllegalArgumentException(unavailableReason());
        Map<String, Object> after = values(text);
        Map<String, Object> changes = TreeSettings.changes(baseline, after);
        if (changes.keySet().stream().anyMatch(p -> p.startsWith("Node style/") && !p.endsWith("/Scope")))
            changes.put("Node style/Scope", after.get("Node style/Scope"));
        TreeSettings.applyToTree(tree, changes);
        baseline = TreeSettings.snapshot(tree, (String) after.get("Node style/Scope"));
    }
}
