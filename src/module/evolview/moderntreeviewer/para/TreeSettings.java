package module.evolview.moderntreeviewer.para;

import com.alibaba.fastjson.JSON;
import egps2.UnifiedAccessPoint;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import evoltree.struct.util.EvolNodeUtil;
import graphic.engine.guicalculator.BlankArea;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.LeafLabelStateInitializer;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;

import java.awt.Font;
import java.awt.Color;
import module.evolview.model.tree.GraphicsNode;
import java.awt.GraphicsEnvironment;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** The editable settings contract for Modern Tree View, independent of model serialization. */
public final class TreeSettings {
    private record Setting(String path, Object initial, String voice, String description,
                           Function<Object, Object> validate, Function<TreeLayoutProperties, Object> read,
                           BiConsumer<TreeLayoutProperties, Object> write, List<String> aliases) { }

    public record Definition(String path, String voice, Object initial, String description, List<String> aliases) { }
    public record InputSetting(String key, String value, String description, List<String> aliases) { }

    public static List<InputSetting> inputSettings() {
        return List.of(
                new InputSetting("input.newick.text", "", "Newick text. Takes priority over file inputs.", List.of("input.nwk.string")),
                new InputSetting("input.newick.file", "", "Newick file path. Used when no Newick text is provided.", List.of("input.nwk.path")),
                new InputSetting("input.table.file", "", "Table-format tree file path. Used when neither Newick source is provided.", List.of("input.tableLike.path")),
                new InputSetting("input.newick.format", Integer.toString(module.evoltreio.NewickFormatConfig.DEFAULT_FORMAT), "Newick format (0-9). 0 reads internal support; 1 reads internal names.", List.of("nwk.format")),
                new InputSetting("input.newick.remove.whitespace", "false", "Remove whitespace before parsing. Leave false to preserve spaces in names.", List.of("nwk.remove.whitespace")),
                new InputSetting("input.node.styles.file", "", "Node-style TSV file. Required column: name. Optional columns: fillColor, circleRadius, lineThickness, displayName. Overrides uniform imported styles.", List.of("advanced.node.visual.config", "node.visual.config.path")));
    }

    /** A missing key differs from an explicitly empty value (notably font.axis). */
    private static boolean supplied(OrganizedParameterGetter input, String key) {
        return input.getComplicatedValues(key).isPresent();
    }

    public static String inputValue(OrganizedParameterGetter input, String canonical) {
        InputSetting setting = inputSettings().stream().filter(s -> s.key.equals(canonical)).findFirst().orElseThrow();
        if (supplied(input, canonical)) return input.getSimplifiedStringWithDefault(canonical);
        for (String alias : setting.aliases) if (supplied(input, alias)) return input.getSimplifiedStringWithDefault(alias);
        return setting.value;
    }

    private static final Map<TreeLayout, String> LAYOUTS = new LinkedHashMap<>();
    private static final List<Setting> SETTINGS = new ArrayList<>();
    private static final String LEAVES = "Labels/Show leaf names";
    private static final String LAYOUT = "Layout/Type";

    static {
        String[] names = {
                "Slant cladogram", "Rectangular cladogram (aligned tips)", "Rectangular cladogram (equal branches)",
                "Rectangular (internal nodes on axis)", "Rectangular phylogram", "Rectangular phylogram (aligned tips)",
                "Rectangular half circle", "Circular cladogram (inward)", "Circular cladogram (aligned tips)",
                "Circular phylogram", "Circular phylogram (aligned tips)", "Circular cladogram (equal branches)",
                "Spiral alpha phylogram", "Spiral alpha cladogram", "Spiral beta phylogram", "Spiral beta cladogram",
                "Radial cladogram", "Radial phylogram", "Radial equal daylight phylogram", "Radial equal daylight cladogram",
                "Radial N-body phylogram", "Radial N-body cladogram"
        };
        TreeLayout[] layouts = TreeLayout.values();
        if (layouts.length != names.length) throw new IllegalStateException("Add a settings name for the new tree layout");
        for (int i = 0; i < layouts.length; i++) LAYOUTS.put(layouts[i], names[i]);
        flag("Display/Show scale bar", true, "tree.show.scale.bar", "Display the branch-length scale where a uniform pixel scale is available. Unavailable in cladograms and Beta spirals.",
                TreeLayoutProperties::isShowScaleBar, TreeLayoutProperties::setShowScaleBar);
        flag("Display/Show axis", true, "tree.show.axis.bar", "Display the coordinate axis where the layout supports it.",
                TreeLayoutProperties::isShowAxisBar, TreeLayoutProperties::setShowAxisBar);
        flag("Display/Show title", true, "tree.show.title", "Display the statement below the tree.", TreeLayoutProperties::isShowTitle, TreeLayoutProperties::setShowTitle);
        flag("Display/Show canvas size", false, "tree.show.width.height", "Show or hide canvas width, height and top, left, bottom and right margins in the upper-left corner (pixels).",
                TreeLayoutProperties::isShowWidthAndHeightString, TreeLayoutProperties::setShowWidthAndHeightString);
        text("Display/Title text", "The phylogenetic tree with {0} high-quality sequenced samples.", "tree.title.string",
                "Bottom statement. {0} is replaced by the number of leaves.", TreeLayoutProperties::getTitleString, TreeLayoutProperties::setTitleString);
        text("Display/Branch length unit", "", "tree.branch.length.unit", "Axis unit, for example MYA. An empty string omits the unit.",
                p -> Objects.toString(p.getBranchLengthUnit(), ""), TreeLayoutProperties::setBranchLengthUnit);
        flag("Display/Reverse axis", false, "tree.need.reverse.axis", "Reverse the axis direction, for example for time before present.",
                TreeLayoutProperties::isNeedReverseAxisBar, TreeLayoutProperties::setNeedReverseAxisBar);
        flag(LEAVES, true, "leaf.show.label", "Display names next to leaf nodes.",
                p -> p.getShowLeafPropertiesInfo().isShowLeafLabel(), LeafLabelStateInitializer::applyInitialLeafLabelState);
        flag("Labels/Right align leaf labels", true, "leaf.label.right.align", "Right align label columns in rectangular and slant layouts.",
                TreeLayoutProperties::isShouldLeafNameRightAlign, TreeLayoutProperties::setShouldLeafNameRightAlign);
        flag("Labels/Show internal node names", false, "inner.show.label", "Display names on internal nodes.",
                p -> p.getShowInnerNodePropertiesInfo().isShowInternalNodeLabel(), (p, value) -> {
                    p.getShowInnerNodePropertiesInfo().setShowInternalNodeLabel(value);
                    EvolNodeUtil.recursiveIterateTreeIF(p.getOriginalRootNode(), n -> {
                        if (n.getChildCount() > 0) n.getDrawUnit().setDrawName(value);
                    });
                });
        flag("Labels/Show bootstrap values", false, "inner.show.bootstrap", "Display bootstrap support values when present in the tree.",
                p -> p.getShowInnerNodePropertiesInfo().isShowInternalNodeBootstrap(),
                (p, value) -> p.getShowInnerNodePropertiesInfo().setShowInternalNodeBootstrap(value));
        flag("Labels/Show branch lengths", false, "inner.show.branch.length", "Display numeric branch lengths.",
                TreeLayoutProperties::isShowNodeBranchLength, TreeLayoutProperties::setShowNodeBranchLength);
        flag("Root/Show root branch", false, "root.show", "Display the branch leading to the root.", TreeLayoutProperties::isShowRoot, TreeLayoutProperties::setShowRoot);
        number("Root/Root branch length", 10.0, 0, 200, "root.tip.length", "Root branch length in pixels (0-200).",
                TreeLayoutProperties::getRootTipLength, (p, v) -> p.setRootTipLength(v.doubleValue()));
        font("Labels", "font.global", UnifiedAccessPoint.getLaunchProperty().getDefaultFont().deriveFont(14f), TreeLayoutProperties::getGlobalFont, TreeLayoutProperties::setGlobalFont);
        font("Title", "font.title", UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont(), TreeLayoutProperties::getTitleFont, TreeLayoutProperties::setTitleFont);
        font("Axis and scale bar", "font.axis", UnifiedAccessPoint.getLaunchProperty().getDefaultFont().deriveFont(14f), TreeLayoutProperties::getAxisFont, TreeLayoutProperties::setAxisFont);
        add(LAYOUT, LAYOUTS.get(TreeLayout.RECT_PHYLO_LEFT), "layout.initial",
                "Tree layout. Choices: " + String.join("; ", LAYOUTS.values()) + ".", value -> {
                    if (!(value instanceof String name)) throw new IllegalArgumentException("Expected a layout name");
                    return LAYOUTS.get(layout(name));
                }, p -> LAYOUTS.get(p.getMyLayout()), (p, value) -> p.setMyLayout(layout((String) value)));
        String[] sides = {"Top", "Left", "Bottom", "Right"};
        int[] margins = {20, 40, 80, 40};
        for (int i = 0; i < sides.length; i++) {
            final int side = i;
            number("Layout/Margins/" + sides[i], margins[i], 0, 10000, "layout.blank.space", "Blank space in pixels (0-10000), excluding automatic label space.",
                    p -> margin(p.getConfiguredBlankArea(), side), (p, value) -> {
                        BlankArea configured = p.getConfiguredBlankArea();
                        int[] values = {configured.getTop(), configured.getLeft(), configured.getBottom(), configured.getRight()};
                        values[side] = value.intValue();
                        p.setBlankArea(new BlankArea(values[0], values[1], values[2], values[3]));
                    });
        }
        number("Rectangular layout/Branch curvature", 0, 0, 100, "layout.rectangular.curvature", "Branch curvature (0-100), rectangular layouts only.",
                p -> p.getRectangularLayoutPropertiy().getCurvature(), (p, v) -> p.getRectangularLayoutPropertiy().setCurvature(v.intValue()));
        number("Circular layout/Start angle", 285, 0, 360, "layout.circular.start.degree", "Start angle in degrees (0-360), circular layouts only.",
                p -> p.getCircularLayoutPropertiy().getGlobalStartDegree(), (p, v) -> p.getCircularLayoutPropertiy().setGlobalStartDegree(v.intValue()));
        number("Circular layout/Arc extent", 360, 0, 360, "layout.circular.extent.degree", "Arc extent in degrees (0-360), circular layouts only.",
                p -> p.getCircularLayoutPropertiy().getGlobalExtendDegree(), (p, v) -> p.getCircularLayoutPropertiy().setGlobalExtendDegree(v.intValue()));
        add("Circular layout/Inner radius ratio", 0.2, "layout.circular.inner.radius.ratio",
                "Inner radius divided by outer radius (0.2-1), inward circular cladogram only. The left panel uses 20-100%. At 1 all nodes share one circle. Old pixel settings and null ratios reset to 0.2.",
                value -> value == null ? 0.2 : numeric(value, 0.2, 1, false),
                p -> p.getCircularLayoutPropertiy().getInnerRadiusRatio(),
                (p, value) -> p.getCircularLayoutPropertiy().setInnerRadiusRatio((Double) value));
        number("Spiral layout/Start angle", 0, 0, 360, "layout.spiral.start.degree", "Start angle in degrees (0-360), spiral layouts only.",
                p -> p.getSprialLayoutPropertiy().getGlobalStartDegree(), (p, v) -> p.getSprialLayoutPropertiy().setGlobalStartDegree(v.intValue()));
        number("Spiral layout/Total angle", 720, 0, 10000, "layout.spiral.extent.degree", "Total spiral angle in degrees (0-10000).",
                p -> p.getSprialLayoutPropertiy().getGlobalExtendingDegree(), (p, v) -> p.getSprialLayoutPropertiy().setGlobalExtendingDegree(v.intValue()));
        number("Spiral layout/Arm spacing", 10, 0, 50, "layout.spiral.gap.factor", "Spacing factor between spiral arms (0-50); this is not a pixel distance.",
                p -> p.getSprialLayoutPropertiy().getGapSize(), (p, v) -> p.getSprialLayoutPropertiy().setGapSize(v.intValue()));
        number("Spiral layout/Beta factor", 1.0, 1, 5, "layout.spiral.beta.factor", "Beta spiral factor (1.0-5.0). Use the actual value, not a percentage.",
                p -> p.getSprialLayoutPropertiy().getBetaFactor(), (p, v) -> p.getSprialLayoutPropertiy().setBetaFactor(v.doubleValue()));
        number("Slant layout/Tree width", 100.0, 0, 100, "layout.slant.tree.width", "Tree width as a percentage (0-100), slant layout only.",
                p -> p.getSlopeLayoutProperety().getLeafLocationRightMostRatio() * 100, (p, v) -> p.getSlopeLayoutProperety().setLeafLocationRightMostRatio(v.doubleValue() / 100));
        number("Slant layout/Left margin", 20.0, 0, 100, "layout.slant.left.margin", "Left margin as a percentage (0-100), slant layout only.",
                p -> p.getSlopeLayoutProperety().getBlankMarginRatio() * 100, (p, v) -> p.getSlopeLayoutProperety().setBlankMarginRatio(v.doubleValue() / 100));
        add("Slant layout/Rotation", 0, "layout.slant.rotation", "Slant rotation in degrees: 0, 90, 180 or 270.", value -> {
            Number n = numeric(value, 0, 270, true);
            if (n.intValue() % 90 != 0) throw new IllegalArgumentException("Use 0, 90, 180 or 270 degrees");
            return n;
        }, p -> p.getSlopeLayoutProperety().getSlopeLayoutRotationDeg(), (p, value) -> p.getSlopeLayoutProperety().setSlopeLayoutRotationDeg(((Number) value).intValue()));
        number("Radial layout/Rotation", 0, 0, 360, "layout.radial.rotation", "Rotation in degrees (0-360), radial layouts only.",
                TreeLayoutProperties::getRadicalLayoutRotationDeg, (p, v) -> p.setRadicalLayoutRotationDeg(v.intValue()));
        flag("Mouse wheel/Zoom height", true, "wheel.height.scale.on", "Allow the mouse wheel to change tree height.", TreeLayoutProperties::isWhetherHeightScaleOnMouseWheel, TreeLayoutProperties::setWhetherHeightScaleOnMouseWheel);
        flag("Mouse wheel/Zoom width", true, "wheel.width.scale.on", "Allow the mouse wheel to change tree width.", TreeLayoutProperties::isWhetherWidthScaleOnMouseWheel, TreeLayoutProperties::setWhetherWidthScaleOnMouseWheel);
    }

    static {
        number("View/Height scale", 100, 100, 100000, "view.height.scale", "Height as a percentage of fitted size (100-100000).", TreeLayoutProperties::getInitialHeightScale, (p, v) -> p.setInitialHeightScale(v.intValue()));
        number("View/Width scale", 100, 100, 100000, "view.width.scale", "Width as a percentage of fitted size (100-100000). Circular, spiral and radial layouts scale uniformly.", TreeLayoutProperties::getInitialWidthScale, (p, v) -> p.setInitialWidthScale(v.intValue()));
        flag("View/Lock aspect ratio", false, "view.lock.aspect.ratio", "Keep the aspect ratio during manual zoom. Circular, spiral and radial layouts always scale uniformly.", TreeLayoutProperties::isLockAspectRatio, TreeLayoutProperties::setLockAspectRatio);
        flag("Fonts/Axis and scale bar/Inherit label font", true, "font.axis", "Use the label font for the axis and scale bar. Empty VOICE font.axis inherits the label font.", TreeLayoutProperties::isAxisFontInherited, TreeLayoutProperties::setAxisFontInherited);
        add("Node style/Scope", "Selected clades", null, "Selected clades includes selected nodes and descendants; without selection, the whole tree. Other choices: All nodes, Leaves only. Scope is not saved.", v -> {
            if (!List.of("Selected clades", "All nodes", "Leaves only").contains(v)) throw new IllegalArgumentException("Use Selected clades, All nodes or Leaves only"); return v;
        }, p -> "Selected clades", (p, v) -> { });
        add("Node style/Color", "#000000", "node.color", "Node and branch color as #RRGGBB, or Mixed. Leave Mixed unchanged to preserve different values.", v -> {
            if (!(v instanceof String s) || !s.matches("#[0-9a-fA-F]{6}|Mixed")) throw new IllegalArgumentException("Use #RRGGBB or Mixed"); return v;
        }, p -> styleValue(p, "Color", "Selected clades"), (p, v) -> applyStyle(p, "Color", v, "All nodes"));
        for (String name : List.of("Branch line width", "Node radius")) {
            boolean radius = name.equals("Node radius");
            add("Node style/" + name, radius ? (Object) 0 : 1.6, radius ? "node.radius" : "node.line.width", radius ? "Node radius in pixels (0-200), or Mixed." : "Branch line width in pixels (0-200), or Mixed.",
                    v -> "Mixed".equals(v) ? v : numeric(v, 0, 200, radius), p -> styleValue(p, name, "Selected clades"), (p, v) -> applyStyle(p, name, v, "All nodes"));
        }
        flag("Creative mode/Enabled", false, "creative.mode", "Drag horizontally to change branch lengths; right-click to edit nodes. Requires a rectangular phylogram. Never saved as enabled in user defaults.", TreeLayoutProperties::isCreativeMode, TreeLayoutProperties::setCreativeMode);
    }

    private TreeSettings() { }

    private static Set<GraphicsNode> targets(TreeLayoutProperties p, String scope) {
        Set<GraphicsNode> nodes = new LinkedHashSet<>();
        List<GraphicsNode> roots = !scope.equals("All nodes") && !p.getSelectedNodes().isEmpty() ? p.getSelectedNodes() : List.of(p.getOriginalRootNode());
        for (GraphicsNode root : roots) EvolNodeUtil.recursiveIterateTreeIF(root, n -> {
            if (!scope.equals("Leaves only") || n.getChildCount() == 0) nodes.add((GraphicsNode) n);
        });
        return nodes;
    }

    private static Object styleValue(TreeLayoutProperties p, String name, String scope) {
        Set<Object> values = new HashSet<>();
        for (GraphicsNode node : targets(p, scope)) {
            var unit = node.getDrawUnit();
            values.add(switch (name) {
                case "Color" -> String.format("#%06X", unit.getLineColor().getRGB() & 0xffffff);
                case "Node radius" -> unit.getCircleRadius();
                default -> Double.valueOf(Float.toString(unit.getStrokeSize()));
            });
        }
        return values.size() == 1 ? values.iterator().next() : "Mixed";
    }

    private static void applyStyle(TreeLayoutProperties p, String name, Object value, String scope) {
        if ("Mixed".equals(value)) return;
        for (GraphicsNode node : targets(p, scope)) switch (name) {
            case "Color" -> node.getDrawUnit().setLineColor(Color.decode((String) value));
            case "Node radius" -> node.getDrawUnit().setCircleRadius(((Number) value).intValue());
            default -> node.getDrawUnit().setStrokeSize(((Number) value).floatValue());
        }
    }

    public static Map<String, Object> snapshot(PhylogeneticTreePanel tree) {
        return snapshot(tree, "Selected clades");
    }

    public static Map<String, Object> snapshot(PhylogeneticTreePanel tree, String scope) {
        Map<String, Object> result = explicitImport(null, tree.getLayoutProperties());
        result.put("Node style/Scope", scope);
        for (String name : List.of("Color", "Branch line width", "Node radius")) result.put("Node style/" + name, styleValue(tree.getLayoutProperties(), name, scope));
        var scale = tree.getTreePanelScale();
        result.put("View/Height scale", scale.height);
        result.put("View/Width scale", scale.width);
        result.put("View/Lock aspect ratio", tree.isUniformScaleLayout() || tree.getLayoutProperties().isLockAspectRatio());
        return result;
    }

    public static void initializeView(PhylogeneticTreePanel tree) {
        var p = tree.getLayoutProperties();
        tree.setTreePanelScale(p.getInitialHeightScale(), p.getInitialWidthScale());
    }

    private static void add(String path, Object initial, String voice, String description, Function<Object, Object> validator,
                            Function<TreeLayoutProperties, Object> read, BiConsumer<TreeLayoutProperties, Object> write) {
        String key = canonical(path, voice);
        SETTINGS.add(new Setting(path, initial, key, description, validator, read, write, aliases(voice).stream().filter(alias -> !alias.equals(key)).toList()));
    }

    private static List<String> aliases(String legacy) {
        if (legacy == null) return List.of();
        String older = switch (legacy) {
            case "font.global" -> "tree.global.font";
            case "font.title" -> "bottom.title.font";
            case "layout.blank.space" -> "blank.space";
            case "wheel.height.scale.on" -> "height.scale.on";
            case "wheel.width.scale.on" -> "width.scale.on";
            case "leaf.show.label" -> "show.leaf.label";
            case "tree.need.reverse.axis" -> "need.reverse.axis.bar";
            case "tree.title.string" -> "bottom.title.string";
            case "tree.branch.length.unit" -> "branch.length.unit";
            default -> null;
        };
        return older == null ? List.of(legacy) : List.of(legacy, older);
    }

    private static String canonical(String path, String legacy) {
        if (path.equals("Node style/Scope")) return "node.style.scope";
        if (path.endsWith("Inherit label font")) return "font.axis.inherit.label.font";
        if (path.startsWith("Fonts/")) {
            String group = legacy.equals("font.global") ? "font.labels" : legacy;
            return group + (path.endsWith("/Family") ? ".family" : path.endsWith("/Size") ? ".size.pt" : ".style");
        }
        if (path.startsWith("Layout/Margins/")) return "layout.margins." + path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT) + ".px";
        return switch (legacy) {
            case "tree.show.scale.bar" -> "display.show.scale.bar";
            case "tree.show.axis.bar" -> "display.show.axis";
            case "tree.show.title" -> "display.show.title";
            case "tree.show.width.height" -> "display.show.canvas.size";
            case "tree.title.string" -> "display.title.text";
            case "tree.branch.length.unit" -> "display.branch.length.unit";
            case "tree.need.reverse.axis" -> "display.reverse.axis";
            case "leaf.show.label" -> "labels.show.leaf.names";
            case "leaf.label.right.align" -> "labels.right.align.leaf.labels";
            case "inner.show.label" -> "labels.show.internal.node.names";
            case "inner.show.bootstrap" -> "labels.show.bootstrap.values";
            case "inner.show.branch.length" -> "labels.show.branch.lengths";
            case "root.show" -> "root.show.branch";
            case "root.tip.length" -> "root.branch.length.px";
            case "layout.initial" -> "layout.type";
            case "layout.rectangular.curvature" -> "layout.rectangular.branch.curvature.percent";
            case "layout.circular.start.degree" -> "layout.circular.start.angle.degrees";
            case "layout.circular.extent.degree" -> "layout.circular.arc.extent.degrees";
            case "layout.circular.inner.radius" -> "layout.circular.inner.radius.px";
            case "layout.spiral.start.degree" -> "layout.spiral.start.angle.degrees";
            case "layout.spiral.extent.degree" -> "layout.spiral.total.angle.degrees";
            case "layout.spiral.gap.factor" -> "layout.spiral.arm.spacing.factor";
            case "layout.spiral.beta.factor" -> "layout.spiral.beta";
            case "layout.slant.tree.width" -> "layout.slant.tree.width.percent";
            case "layout.slant.left.margin" -> "layout.slant.left.margin.percent";
            case "layout.slant.rotation" -> "layout.slant.rotation.degrees";
            case "layout.radial.rotation" -> "layout.radial.rotation.degrees";
            case "wheel.height.scale.on" -> "mouse.wheel.zoom.height";
            case "wheel.width.scale.on" -> "mouse.wheel.zoom.width";
            case "view.height.scale" -> "view.height.scale.percent";
            case "view.width.scale" -> "view.width.scale.percent";
            case "node.color" -> "node.style.color";
            case "node.radius" -> "node.style.radius.px";
            case "node.line.width" -> "node.style.branch.line.width.px";
            case "creative.mode" -> "creative.mode.enabled";
            default -> legacy;
        };
    }

    public static List<Definition> definitions() {
        return SETTINGS.stream().map(s -> new Definition(s.path, s.voice, s.initial, s.description, s.aliases)).toList();
    }

    public static String tooltip(String path) {
        Setting setting = SETTINGS.stream().filter(s -> s.path.equals(path)).findFirst().orElseThrow();
        return setting.description + " VOICE: " + setting.voice;
    }

    public static String referenceMarkdown(boolean chinese) {
        StringBuilder text = new StringBuilder(chinese ? "# Modern Tree View 参数对照\n\n" : "# Modern Tree View parameter reference\n\n");
        text.append(chinese
                ? "由参数定义表生成。高级参数路径和 JSONC 文件保持不变；新模板只使用规范 VOICE key，旧脚本无需重写。\n\n优先级：程序默认 → 用户默认 → VOICE 显式参数 → 当前树修改。单个字段中：规范 key → 当前旧名 → 更早历史旧名。\n\n"
                : "Generated from the settings registry. Advanced parameters paths and existing JSONC files remain unchanged. New templates use canonical VOICE keys; existing scripts do not need rewriting.\n\nPrecedence: program defaults → user defaults → explicit VOICE → current tree edits. Per field: canonical key → current legacy key → older alias.\n\n");
        text.append("| Advanced parameters path | Canonical VOICE key | Legacy aliases | Program default | Meaning and constraints |\n|---|---|---|---|---|\n");
        for (Setting setting : SETTINGS) {
            text.append("| ").append(setting.path).append(" | `").append(setting.voice).append("` | ")
                    .append(String.join(", ", setting.aliases)).append(" | `").append(JSON.toJSONString(setting.initial))
                    .append("` | ").append(setting.description.replace("|", "\\|")).append(" |\n");
        }
        text.append("\n## Import-only parameters\n\n| Canonical VOICE key | Legacy aliases | Default | Meaning |\n|---|---|---|---|\n");
        for (InputSetting setting : inputSettings()) text.append("| `").append(setting.key).append("` | ")
                .append(String.join(", ", setting.aliases)).append(" | `").append(setting.value).append("` | ").append(setting.description).append(" |\n");
        text.append("\n## Compatibility details\n\n")
                .append("- `layout.circular.inner.radius.ratio=0.2` uses 20% of the outer radius. The left panel accepts 20-100%; 100% collapses radial branches onto one circle. Legacy `layout.circular.inner.radius.px`, `layout.circular.inner.radius`, old JSONC Inner radius and null ratios migrate to 20%. An explicit ratio takes priority. Current snapshots contain only the ratio.\n")
                .append("- `layout.spiral.beta=1.75` is equivalent to legacy `layout.spiral.beta.factor=175`. The old key retains its 0.01 unit.\n")
                .append("- Legacy margins use `top,left,bottom,right` pixels. Canonical single-side keys override only their side.\n")
                .append("- Legacy packed font keys are expanded before canonical family/size/style values. Explicit `font.axis.inherit.label.font` has final precedence. Empty legacy `font.axis` inherits labels; explicit axis attributes disable implicit inheritance.\n")
                .append("- Layout accepts the listed English names, existing enum names, and RECTANGULAR/CIRCULAR/SPIRAL/SLANT/RADIAL. Unknown names are errors.\n")
                .append("- Boolean examples use true/false; legacy T/F remain accepted. Fonts use Regular, Bold, Italic, or Bold Italic.\n")
                .append("- Selected clades includes selected nodes and descendants; without selection it means the whole tree. Leaves only limits that scope to leaves. Import has no selection.\n")
                .append("- Apply changes the current tree only. Save as user defaults stores reusable preferences separately. Scope, Mixed values and creative-mode activation are not saved.\n")
                .append("- Data sources, Newick options, and node-style files belong to import, not the current tree's display snapshot. Per-node annotations override imported uniform styles.\n");
        return text.toString();
    }

    private static void flag(String path, boolean initial, String voice, String description, Function<TreeLayoutProperties, Boolean> read, BiConsumer<TreeLayoutProperties, Boolean> write) {
        add(path, initial, voice, description + " Use true or false.", value -> {
            if (!(value instanceof Boolean)) throw new IllegalArgumentException("Expected true or false");
            return value;
        }, read::apply, (p, value) -> write.accept(p, (Boolean) value));
    }

    private static void text(String path, String initial, String voice, String description, Function<TreeLayoutProperties, String> read, BiConsumer<TreeLayoutProperties, String> write) {
        add(path, initial, voice, description, value -> {
            if (!(value instanceof String)) throw new IllegalArgumentException("Expected a double-quoted string");
            if (path.equals("Display/Title text")) new java.text.MessageFormat((String) value);
            return value;
        }, read::apply, (p, value) -> write.accept(p, (String) value));
    }

    private static void number(String path, Number initial, double min, double max, String voice, String description,
                               Function<TreeLayoutProperties, Number> read, BiConsumer<TreeLayoutProperties, Number> write) {
        add(path, initial, voice, description, value -> numeric(value, min, max, initial instanceof Integer), read::apply, (p, value) -> write.accept(p, (Number) value));
    }

    private static Number numeric(Object value, double min, double max, boolean integer) {
        if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue() < min || n.doubleValue() > max
                || (integer && n.doubleValue() != Math.rint(n.doubleValue()))) {
            throw new IllegalArgumentException("Expected " + (integer ? "an integer" : "a number") + " from " + min + " to " + max);
        }
        if (integer) return ((Number) value).intValue();
        return ((Number) value).doubleValue();
    }

    private static void font(String name, String voice, Font initial, Function<TreeLayoutProperties, Font> read, BiConsumer<TreeLayoutProperties, Font> write) {
        String path = "Fonts/" + name + "/";
        add(path + "Family", initial.getFamily(), voice, "Installed font family or Java logical font, for example Monospaced.", value -> {
            if (!(value instanceof String family) || Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).noneMatch(family::equalsIgnoreCase)) {
                throw new IllegalArgumentException("Expected an installed font family");
            }
            return value;
        }, p -> read.apply(p).getFamily(), (p, value) -> write.accept(p, new Font((String) value, read.apply(p).getStyle(), 1).deriveFont(read.apply(p).getSize2D())));
        number(path + "Size", (double) initial.getSize2D(), 1, 200, voice, "Font size in points (1-200).", p -> read.apply(p).getSize2D(), (p, v) -> write.accept(p, read.apply(p).deriveFont(v.floatValue())));
        List<String> styles = List.of("Regular", "Bold", "Italic", "Bold Italic");
        add(path + "Style", styles.get(initial.getStyle()), voice, "Font style: Regular, Bold, Italic or Bold Italic.", value -> {
            if (!styles.contains(value)) throw new IllegalArgumentException("Use Regular, Bold, Italic or Bold Italic");
            return value;
        }, p -> styles.get(read.apply(p).getStyle()), (p, value) -> write.accept(p, read.apply(p).deriveFont(styles.indexOf(value), read.apply(p).getSize2D())));
    }

    private static int margin(BlankArea area, int side) {
        return switch (side) { case 0 -> area.getTop(); case 1 -> area.getLeft(); case 2 -> area.getBottom(); default -> area.getRight(); };
    }

    private static TreeLayout layout(String name) {
        for (var entry : LAYOUTS.entrySet()) if (entry.getValue().equalsIgnoreCase(name.trim())) return entry.getKey();
        String enumName = switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "RECTANGULAR" -> "RECT_PHYLO_LEFT";
            case "CIRCULAR" -> "OUTER_CIR_PHYLO";
            case "SPIRAL" -> "SPRIAL_ALPHA_PHYLO";
            case "SLANT" -> "SLOPE_CLADO_ALIGNED_LEFT";
            case "RADIAL" -> "RADIAL_PHYLO";
            default -> name.trim().toUpperCase(Locale.ROOT);
        };
        try { return TreeLayout.valueOf(enumName); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("Unknown layout: " + name + ". Choose a layout listed in Default settings."); }
    }

    public static Map<String, Object> defaults() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Setting setting : SETTINGS) result.put(setting.path, setting.initial);
        return result;
    }

    public static Map<String, Object> parse(String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        flatten(SettingsJson.parse(text), "", result);
        return normalizeLegacyInnerRadius(result);
    }

    static Map<String, Object> normalizeLegacyInnerRadius(Map<String, Object> values) {
        Map<String, Object> result = new LinkedHashMap<>(values);
        if (result.containsKey("Circular layout/Inner radius")) {
            numeric(result.remove("Circular layout/Inner radius"), 0, 200, true);
            result.putIfAbsent("Circular layout/Inner radius ratio", 0.2);
        }
        return result;
    }

    private static void flatten(Map<?, ?> object, String parent, Map<String, Object> result) {
        for (Map.Entry<?, ?> entry : object.entrySet()) {
            String path = parent.isEmpty() ? entry.getKey().toString() : parent + "/" + entry.getKey();
            if (entry.getKey().toString().contains("/")) throw new IllegalArgumentException("Unknown setting: " + path);
            Setting setting = SETTINGS.stream().filter(s -> s.path.equals(path)).findFirst().orElse(null);
            try {
                if (path.equals("Circular layout/Inner radius")) result.put(path, numeric(entry.getValue(), 0, 200, true));
                else if (setting != null) result.put(path, setting.validate.apply(entry.getValue()));
                else if (SETTINGS.stream().anyMatch(s -> s.path.startsWith(path + "/")) && entry.getValue() instanceof Map<?, ?> group) flatten(group, path, result);
                else throw new IllegalArgumentException("Unknown setting or invalid group");
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(path + ": " + error.getMessage(), error);
            }
        }
    }

    public static Map<String, Object> explicitImport(OrganizedParameterGetter input, TreeLayoutProperties parsed) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Setting setting : SETTINGS) {
            if (input == null) { result.put(setting.path, setting.read.apply(parsed)); continue; }
            String key = supplied(input, setting.voice) ? setting.voice : setting.aliases.stream().filter(k -> supplied(input, k)).findFirst().orElse(null);
            if (key == null) continue;
            String raw = input.getSimplifiedStringWithDefault(key);
            Object value;
            try {
                if (List.of("font.global", "tree.global.font", "font.title", "bottom.title.font", "font.axis").contains(key)) {
                    if (setting.path.endsWith("Inherit label font")) value = raw.isEmpty();
                    else if (raw.isEmpty()) continue;
                    else {
                        Font font = egps2.utils.common.util.EGPSFonts.parseFont(raw);
                        value = setting.path.endsWith("/Family") ? font.getFamily() : setting.path.endsWith("/Size") ? (double) font.getSize2D() : List.of("Regular", "Bold", "Italic", "Bold Italic").get(font.getStyle());
                    }
                } else if (key.equals("layout.blank.space") || key.equals("blank.space")) {
                    String[] parts = raw.split(",", -1);
                    if (parts.length != 4) throw new IllegalArgumentException("Expected top,left,bottom,right in pixels");
                    int side = List.of("Top", "Left", "Bottom", "Right").indexOf(setting.path.substring(setting.path.lastIndexOf('/') + 1));
                    for (String part : parts) numeric(Double.valueOf(part.trim()), 0, 10000, true);
                    value = Double.valueOf(parts[side].trim());
                }
                else if (setting.initial instanceof Boolean) {
                    Boolean bool = org.apache.commons.lang3.BooleanUtils.toBooleanObject(raw);
                    if (bool == null) throw new IllegalArgumentException("Use T, F, true or false");
                    value = bool;
                } else if (setting.path.equals("Circular layout/Inner radius ratio") && raw.equals("null")) {
                    value = null;
                } else if (setting.initial instanceof Number) {
                    value = Double.valueOf(raw);
                    if (key.equals("layout.spiral.beta.factor")) value = ((Number) value).doubleValue() / 100;
                } else value = raw;
                if (setting.path.startsWith("Node style/") && "Mixed".equals(value)) throw new IllegalArgumentException("Mixed is a snapshot value, not a VOICE setting");
                result.put(setting.path, setting.validate.apply(value));
            } catch (IllegalArgumentException error) { throw new IllegalArgumentException(key + " (" + setting.path + "): " + error.getMessage(), error); }
        }
        if (input != null && !result.containsKey("Circular layout/Inner radius ratio")) {
            for (String legacy : List.of("layout.circular.inner.radius.px", "layout.circular.inner.radius")) {
                if (supplied(input, legacy)) {
                    numeric(Double.valueOf(input.getSimplifiedStringWithDefault(legacy)), 0, 200, true);
                    result.put("Circular layout/Inner radius ratio", 0.2);
                    break;
                }
            }
        }
        if (input != null && !supplied(input, "font.axis.inherit.label.font")
                && result.keySet().stream().anyMatch(p -> p.startsWith("Fonts/Axis and scale bar/") && !p.endsWith("Inherit label font")))
            result.put("Fonts/Axis and scale bar/Inherit label font", false);
        return result;
    }

    public record VoiceSetting(String key, String value, String comment) { }

    public static List<VoiceSetting> voiceSettings() {
        Map<String, VoiceSetting> result = new LinkedHashMap<>();
        for (Setting setting : SETTINGS) {
            String value = setting.initial.toString();
            if (setting.initial instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) value = Integer.toString(n.intValue());
            result.put(setting.voice, new VoiceSetting(setting.voice, value, setting.path + ": " + setting.description));
        }
        return new ArrayList<>(result.values());
    }

    public static void apply(TreeLayoutProperties properties, Map<String, Object> values) {
        values = normalizeLegacyInnerRadius(values);
        TreeLayout target = values.containsKey(LAYOUT) ? layout((String) values.get(LAYOUT)) : properties.getMyLayout();
        if (Boolean.TRUE.equals(values.get("Creative mode/Enabled")) && target != TreeLayout.RECT_PHYLO_LEFT && target != TreeLayout.RECT_PHYLO_ALIGN2TIP)
            throw new IllegalArgumentException("Creative mode requires a rectangular phylogram.");
        Map<String, Object> checked = new LinkedHashMap<>();
        for (Setting setting : SETTINGS) if (values.containsKey(setting.path)) {
            try { checked.put(setting.path, setting.validate.apply(values.get(setting.path))); }
            catch (IllegalArgumentException error) { throw new IllegalArgumentException(setting.path + ": " + error.getMessage(), error); }
        }
        if (checked.keySet().stream().anyMatch(p -> p.startsWith("Layout/Margins/"))) properties.synchronizeColumnLabelReserve(0);
        for (Setting setting : SETTINGS) if (checked.containsKey(setting.path) && !setting.path.startsWith("Node style/")) setting.write.accept(properties, checked.get(setting.path));
        String scope = (String) checked.getOrDefault("Node style/Scope", "All nodes");
        for (String name : List.of("Color", "Branch line width", "Node radius"))
            if (checked.containsKey("Node style/" + name)) applyStyle(properties, name, checked.get("Node style/" + name), scope);
    }

    public static void applyToTree(PhylogeneticTreePanel tree, Map<String, Object> changes) {
        if (tree == null || changes.isEmpty()) return;
        TreeLayoutProperties properties = tree.getLayoutProperties();
        if (changes.size() == 1 && changes.containsKey("Display/Show scale bar")) {
            apply(properties, changes);
            tree.notifyTreeViewStateChanged();
            tree.refreshViewPort();
            return;
        }
        TreeLayout targetLayout = changes.containsKey(LAYOUT) ? layout((String) changes.get(LAYOUT)) : properties.getMyLayout();
        if (Boolean.TRUE.equals(changes.get("Creative mode/Enabled")) && targetLayout != TreeLayout.RECT_PHYLO_LEFT && targetLayout != TreeLayout.RECT_PHYLO_ALIGN2TIP)
            throw new IllegalArgumentException("Creative mode/Enabled: Requires a rectangular phylogram.");
        var scale = tree.getTreePanelScale();
        if (changes.keySet().stream().anyMatch(s -> s.startsWith("Layout/Margins/"))) properties.synchronizeColumnLabelReserve(0);
        Map<String, Object> ordinary = new LinkedHashMap<>(changes);
        ordinary.remove(LAYOUT);
        ordinary.remove(LEAVES);
        ordinary.keySet().removeIf(key -> key.startsWith("Node style/") || key.equals("Creative mode/Enabled"));
        apply(properties, ordinary);
        if (changes.containsKey(LAYOUT)) tree.updateViewAccording2TreeLayout(layout((String) changes.get(LAYOUT)));
        if (changes.containsKey(LEAVES)) tree.setLeafLabelsVisible((Boolean) changes.get(LEAVES));
        String scope = (String) changes.getOrDefault("Node style/Scope", "Selected clades");
        for (String name : List.of("Color", "Branch line width", "Node radius"))
            if (changes.containsKey("Node style/" + name)) applyStyle(properties, name, changes.get("Node style/" + name), scope);
        if (changes.containsKey("Creative mode/Enabled")) properties.setCreativeMode((Boolean) changes.get("Creative mode/Enabled"));
        tree.recalculatePreservingZoom();
        if (changes.containsKey("View/Height scale") || changes.containsKey("View/Width scale")) {
            int height = ((Number) changes.getOrDefault("View/Height scale", scale.height)).intValue();
            int width = ((Number) changes.getOrDefault("View/Width scale", scale.width)).intValue();
            if (properties.isLockAspectRatio() && !tree.isUniformScaleLayout()
                    && changes.containsKey("View/Height scale") != changes.containsKey("View/Width scale")) {
                double factor = changes.containsKey("View/Height scale") ? height / (double) scale.height : width / (double) scale.width;
                factor = Math.max(factor, Math.max(100.0 / scale.height, 100.0 / scale.width));
                factor = Math.min(factor, Math.min(100000.0 / scale.height, 100000.0 / scale.width));
                height = (int) Math.round(scale.height * factor);
                width = (int) Math.round(scale.width * factor);
            }
            tree.setTreePanelScale(height, width);
        }
        tree.notifyTreeViewStateChanged();
    }

    public static Map<String, Object> changes(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Setting setting : SETTINGS) {
            Object oldValue = before.getOrDefault(setting.path, setting.initial);
            Object newValue = after.getOrDefault(setting.path, setting.initial);
            boolean same = oldValue instanceof Number a && newValue instanceof Number b
                    ? Double.compare(a.doubleValue(), b.doubleValue()) == 0 : Objects.equals(oldValue, newValue);
            if (!same || before.containsKey(setting.path) != after.containsKey(setting.path)) result.put(setting.path, newValue);
        }
        return result;
    }

    public static String defaultText() {
        return documentText(defaults(), "// Program defaults.\n// See each setting's comments for its meaning and VOICE key.\n");
    }

    public static String documentText(Map<String, Object> values) {
        return documentText(values, "// Current parameters.\n// Apply updates this tree.\n// Save as user defaults stores reusable settings separately.\n");
    }

    private static String documentText(Map<String, Object> values, String header) {
        Map<String, Object> tree = new LinkedHashMap<>();
        for (Setting setting : SETTINGS) {
            Map<String, Object> group = tree;
            String[] parts = setting.path.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                @SuppressWarnings("unchecked") Map<String, Object> next = (Map<String, Object>) group.computeIfAbsent(parts[i], ignored -> new LinkedHashMap<>());
                group = next;
            }
            group.put(parts[parts.length - 1], new Setting(setting.path, values.getOrDefault(setting.path, setting.initial), setting.voice,
                    setting.description + " VOICE: " + setting.voice + ".", setting.validate, setting.read, setting.write, setting.aliases));
        }
        StringBuilder result = new StringBuilder(header);
        document(tree, result, 0);
        return result.append('\n').toString();
    }

    private static void document(Map<String, Object> object, StringBuilder result, int depth) {
        result.append("{\n");
        int index = 0;
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            String indent = "    ".repeat(depth + 1);
            if (entry.getValue() instanceof Setting setting) {
                int length = 0;
                result.append(indent).append("// ");
                for (String word : setting.description.split(" ")) {
                    if (length + word.length() > 66 - indent.length() - 3) { result.append('\n').append(indent).append("// "); length = 0; }
                    result.append(word).append(' ');
                    length += word.length() + 1;
                }
                result.append('\n').append(indent).append(JSON.toJSONString(entry.getKey())).append(": ").append(JSON.toJSONString(setting.initial));
            } else {
                result.append(indent).append(JSON.toJSONString(entry.getKey())).append(": ");
                @SuppressWarnings("unchecked") Map<String, Object> group = (Map<String, Object>) entry.getValue();
                document(group, result, depth + 1);
            }
            if (++index < object.size()) result.append(',');
            result.append('\n');
        }
        result.append("    ".repeat(depth)).append('}');
    }
}
