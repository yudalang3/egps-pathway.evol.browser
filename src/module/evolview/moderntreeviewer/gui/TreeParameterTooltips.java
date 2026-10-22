package module.evolview.moderntreeviewer.gui;

import module.evolview.moderntreeviewer.para.TreeSettings;
import javax.swing.*;
import java.awt.*;

/** Module-specific help, derived from the same definitions as VOICE and Advanced parameters. */
public final class TreeParameterTooltips {
    private TreeParameterTooltips() { }

    public static String scaleHelp(boolean axis, boolean hasTree, boolean available, String reason) {
        String title = axis ? "Branch-length coordinate axis" : "Branch-length scale";
        String status = !hasTree ? "Load a tree first" : available ? "Click to show or hide" : "Unavailable in this view";
        String explanation = !hasTree ? "Import a tree to use this control."
                : available ? axis ? "Read branch-length values along the coordinate axis."
                        : "The line represents the branch-length distance printed below it."
                : reason;
        String body = escape(explanation);
        for (String action : new String[] {"Switch to a phylogram", "Use its numeric spiral", "Use the branch-length scale"})
            body = body.replace(". " + action, ".<br><br><b>Next step:</b> " + action);
        return "<html><table width='400' cellpadding='3'><tr><td><b>" + title
                + "</b><br><b>" + status + "</b><br><br>" + body
                + "<br><br><font size='-1'>VOICE: " + (axis ? "display.show.axis.bar" : "display.show.scale.bar")
                + "</font></td></tr></table></html>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static void install(Container container, boolean hasTree) {
        if (container == null) return;
        for (Component component : container.getComponents()) {
            if (component instanceof JComponent control && control.getClientProperty("tree.parameter") instanceof String path) {
                if (control.getClientProperty("tree.original.help") == null)
                    control.putClientProperty("tree.original.help", control.getToolTipText() == null ? "" : control.getToolTipText());
                String text = TreeSettings.tooltip(path);
                if (path.equals("Creative mode/Enabled")) text = control.getClientProperty("tree.action.help") + " VOICE: creative.mode.enabled";
                if (path.startsWith("Fonts/") && control instanceof AbstractButton) {
                    String group = path.substring(0, path.lastIndexOf('/'));
                    String keys = TreeSettings.definitions().stream().filter(d -> d.path().startsWith(group + "/") && !d.path().endsWith("Inherit label font"))
                            .map(TreeSettings.Definition::voice).collect(java.util.stream.Collectors.joining(", "));
                    text = "Choose font family, size in points and style for " + group.substring("Fonts/".length()).toLowerCase(java.util.Locale.ROOT) + ". VOICE: " + keys;
                    if (path.startsWith("Fonts/Axis")) text += ". Choosing an axis font disables label-font inheritance.";
                }
                if (path.equals("Layout/Type")) {
                    String label = control instanceof AbstractButton button ? button.getText() : "";
                    if (label == null || label.isBlank()) label = (String) control.getClientProperty("tree.original.help");
                    String effect = label.equals("Align tips") ? "Align displayed leaf tips without changing stored branch lengths."
                            : label.equals("Phylogram") ? "Display branch lengths proportionally to their stored values."
                            : label.contains("Cladogram") ? "Display topology with the selected cladogram spacing; keep stored branch lengths."
                            : label.equals("Alpha") ? "Use an Archimedean Alpha spiral."
                            : label.equals("Beta") ? "Use a Beta spiral with the configured growth factor."
                            : label.equals("Equal-angle") ? "Use equal-angle branch spacing in an unrooted tree."
                            : label.equals("Equal-daylight") ? "Balance the open angles around branches in an unrooted tree."
                            : label.equals("N-body") ? "Use an iterative force-based unrooted layout. Calculation may take longer; use Cancel N-body calculation to stop."
                            : "Select " + label + ".";
                    text = effect + " VOICE: layout.type";
                } else if (path.startsWith("Node style/") && control instanceof AbstractButton) {
                    String original = (String) control.getClientProperty("tree.original.help");
                    String verb = original.toLowerCase().contains("decrease") ? "Decrease" : "Increase";
                    text = path.endsWith("Color") ? "Choose a node and branch color. Apply to selected nodes and descendants, or the whole tree when nothing is selected. VOICE: node.style.color"
                            : verb + (path.endsWith("Node radius") ? " node radius." : " branch line width.")
                            + " Left-click: selected nodes and descendants, or the whole tree without a selection. Right-click: leaves in that scope. "
                            + TreeSettings.tooltip(path);
                }
                if (path.equals("Spiral layout/Beta factor")) text = "The GUI spinner uses hundredths: 175 means 1.75. The canonical VOICE key uses the actual value. " + text;
                if (path.equals("Circular layout/Inner radius ratio")) text = "The left panel uses percentages: 20 means a ratio of 0.2. " + text;
                if (!hasTree) text = "Load a tree to use this control. " + text;
                else if (!control.isEnabled()) {
                    String reason = path.equals("Display/Show scale bar") ? "A scale bar is unavailable in this layout. "
                            : path.equals("Display/Show axis") ? "An axis is unavailable in this layout. "
                            : path.equals("Labels/Show bootstrap values") ? "This tree has no bootstrap values. "
                            : path.equals("Root/Root branch length") ? "Enable Show root branch to adjust this value. "
                            : path.equals("Spiral layout/Beta factor") ? "Select a Beta spiral to use this factor. "
                            : path.equals("Circular layout/Inner radius ratio") ? "Select Inner cladogram to adjust the percentage. "
                            : path.equals("View/Lock aspect ratio") ? "This layout always uses uniform scaling. " : "";
                    if (control.getClientProperty("tree.unavailable.reason") instanceof String detail
                            && (path.equals("Display/Show scale bar") || path.equals("Display/Show axis"))) reason = detail + " ";
                    text = reason + text;
                }
                String help = path.equals("Display/Show scale bar") || path.equals("Display/Show axis")
                        ? scaleHelp(path.equals("Display/Show axis"), hasTree, control.isEnabled(),
                                java.util.Objects.toString(control.getClientProperty("tree.unavailable.reason"), text))
                        : wrap(text);
                control.setToolTipText(help);
                if (control instanceof JSpinner spinner) {
                    childHelp(spinner, help);
                }
            }
            if (component instanceof Container child) install(child, hasTree);
        }
    }

    private static void childHelp(Container container, String help) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComponent child) child.setToolTipText(help);
            if (component instanceof Container child) childHelp(child, help);
        }
    }

    public static void setAvailable(Container container, boolean available) {
        if (container == null) return;
        for (Component component : container.getComponents()) {
            if (component instanceof AbstractButton || component instanceof JSpinner || component instanceof JSlider
                    || component instanceof egps2.frame.gui.comp.toggle.toggle.ToggleButton) component.setEnabled(available);
            if (component instanceof Container child && !(component instanceof JSpinner)) setAvailable(child, available);
        }
    }

    private static String wrap(String text) {
        StringBuilder result = new StringBuilder("<html>");
        int length = 0;
        for (String word : text.split(" ")) {
            if (length + word.length() > 85) { result.append("<br>"); length = 0; }
            result.append(word.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")).append(' ');
            length += word.length() + 1;
        }
        return result.append("</html>").toString();
    }
}
