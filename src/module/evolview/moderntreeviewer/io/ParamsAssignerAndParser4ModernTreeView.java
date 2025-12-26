package module.evolview.moderntreeviewer.io;

import com.google.common.base.Strings;
import egps2.UnifiedAccessPoint;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.utils.common.util.EGPSFonts;
import graphic.engine.guicalculator.BlankArea;
import module.evoltreio.ParamsAssignerAndParser4PhyloTree;
import org.apache.commons.lang3.BooleanUtils;
import utils.string.EGPSStringUtil;

import java.awt.*;

public class ParamsAssignerAndParser4ModernTreeView extends ParamsAssignerAndParser4PhyloTree {

	public ParamsAssignerAndParser4ModernTreeView() {
		super();

		Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont().deriveFont(14f);
		Font defaultTitleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();
		String codeFont = EGPSFonts.codeFont(defaultFont);
		String codeTitleFont = EGPSFonts.codeFont(defaultTitleFont);

		// Category %1: Tree Information
		addKeyValueEntryBean("%1", "Tree Information", "");
		addKeyValueEntryBean("tree.show.scale.bar", "F", "Show the plotting scale for the phylogram.");
		addKeyValueEntryBean("tree.show.axis.bar", "T", "Show the x-axis.");
		addKeyValueEntryBean("tree.show.title", "T", "Show the statement at the bottom of the tree.");
		addKeyValueEntryBean("tree.show.width.height", "F",
				"Show width and height in the drawing panel. Useful for circular layout.");
		addKeyValueEntryBean("tree.title.string",
				"The phylogenetic tree with {0} high-quality sequenced samples.",
				"Statement displayed at the bottom. {0} will be replaced with number of leaves.");
		addKeyValueEntryBean("tree.branch.length.unit", "",
				"The unit of branch, for example mya(million years ago) or evolutionary rate.");
		addKeyValueEntryBean("tree.need.reverse.axis", "F",
				"Whether reverse the axis bar. In some cases, the time is years ago.");

		// Category %2: Leaf Node
		addKeyValueEntryBean("%2", "Leaf Node", "");
		addKeyValueEntryBean("leaf.show.label", "T", "Whether display the leaf label on the tree.");
		addKeyValueEntryBean("leaf.label.right.align", "T", "Whether right align the leaf labels.");

		// Category %3: Inner Node
		addKeyValueEntryBean("%3", "Inner Node", "");
		addKeyValueEntryBean("inner.show.label", "F", "Show internal node label.");
		addKeyValueEntryBean("inner.show.bootstrap", "F", "Show internal node bootstrap value.");
		addKeyValueEntryBean("inner.show.branch.length", "F", "Show node branch length.");

		// Category %4: Root Settings
		addKeyValueEntryBean("%4", "Root Settings", "");
		addKeyValueEntryBean("root.show", "F", "Whether to display the root node.");
		addKeyValueEntryBean("root.tip.length", "10", "The length of the root tip line in pixels.");

		// Category %5: Mouse Wheel
		addKeyValueEntryBean("%5", "Mouse Wheel", "");
		addKeyValueEntryBean("wheel.height.scale.on", "T", "Whether mouse wheel scale on for height.");
		addKeyValueEntryBean("wheel.width.scale.on", "T", "Whether mouse wheel scale on for width.");

		// Category %6: Font Settings
		addKeyValueEntryBean("%6", "Font Settings", "");
		addKeyValueEntryBean("font.global", codeFont, "The font for global, leaf label names employ it.");
		addKeyValueEntryBean("font.title", codeTitleFont, "The font for bottom title.");
		addKeyValueEntryBean("font.axis", "", "The font for x-axis and plotting scale. Leave empty to use global font.");

		// Category %7: Layout Settings
		addKeyValueEntryBean("%7", "Layout Settings", "");
		addKeyValueEntryBean("layout.blank.space", "20,40,80,40", "The blank area of top,left,bottom,right (pixels).");

		// Category %8: Advanced
		addKeyValueEntryBean("%8", "Advanced", "");
		addKeyValueEntryBean("advanced.node.visual.config", "",
				"Node specific visual annotations, current supported(Note, only fixed symbols will be recognized): name, fillColor, circleRadius, lineThickness, displayName.\n"
						+ "# Please input a tsv file with header. For e.g (remove # in real file):\n"
						+ "#name\tfillColor\tcircleRadius\tlineThickness\tdisplayName\n"
						+ "#leaf1\t#FF00FF\t2\t2\tT\n"
						+ "#leaf2\t12,00,34\t1\t2\tF");

	}

	@Override
	public MTVImportInforBean generateTreeFromKeyValue(OrganizedParameterGetter input) {
		MTVImportInforBean ret = new MTVImportInforBean();
		super.assignValues(input, ret);

		String string;

		// Category %1: Tree Information
		string = input.getSimplifiedStringWithDefault("tree.show.scale.bar");
		if (!string.isEmpty()) {
			ret.setShowScaleBar(BooleanUtils.toBoolean(string));
		}
		string = input.getSimplifiedStringWithDefault("tree.show.axis.bar");
		if (!string.isEmpty()) {
			ret.setShowAxisBar(BooleanUtils.toBoolean(string));
		}
		string = input.getSimplifiedStringWithDefault("tree.show.title");
		if (!string.isEmpty()) {
			ret.setShowTitle(BooleanUtils.toBoolean(string));
		}
		string = input.getSimplifiedStringWithDefault("tree.show.width.height");
		if (!string.isEmpty()) {
			ret.setShowWidthAndHeightString(BooleanUtils.toBoolean(string));
		}
		string = input.getSimplifiedStringWithDefault("tree.title.string");
		if (!string.isEmpty()) {
			ret.setTitleString(string);
		}
		string = input.getSimplifiedStringWithDefault("tree.branch.length.unit");
		if (!string.isEmpty()) {
			ret.setBranchLengthUnit(string);
		}
		string = input.getSimplifiedStringWithDefault("tree.need.reverse.axis");
		if (!string.isEmpty()) {
			ret.setNeedReverseAxisBar(BooleanUtils.toBoolean(string));
		}

		// Category %2: Leaf Node
		string = input.getSimplifiedStringWithDefault("leaf.show.label");
		if (!string.isEmpty()) {
			ret.setShowLeafLabel(BooleanUtils.toBoolean(string));
		}
		string = input.getSimplifiedStringWithDefault("leaf.label.right.align");
		if (!string.isEmpty()) {
			ret.setShouldLeafNameRightAlign(BooleanUtils.toBoolean(string));
		}

		// Category %3: Inner Node
		string = input.getSimplifiedStringWithDefault("inner.show.label");
		if (!string.isEmpty()) {
			ret.setShowInnerNodeLabel(BooleanUtils.toBoolean(string));
		}
		string = input.getSimplifiedStringWithDefault("inner.show.bootstrap");
		if (!string.isEmpty()) {
			ret.setShowBootstrap(BooleanUtils.toBoolean(string));
		}
		string = input.getSimplifiedStringWithDefault("inner.show.branch.length");
		if (!string.isEmpty()) {
			ret.setShowBranchLength(BooleanUtils.toBoolean(string));
		}

		// Category %4: Root Settings
		string = input.getSimplifiedStringWithDefault("root.show");
		if (!string.isEmpty()) {
			ret.setShowRoot(BooleanUtils.toBoolean(string));
		}
		string = input.getSimplifiedStringWithDefault("root.tip.length");
		if (!string.isEmpty()) {
			ret.setRootTipLength(Double.parseDouble(string));
		}

		// Category %5: Mouse Wheel
		string = input.getSimplifiedStringWithDefault("wheel.height.scale.on");
		if (!string.isEmpty()) {
			ret.setWhetherHeightScaleOnMouseWheel(BooleanUtils.toBoolean(string));
		}
		string = input.getSimplifiedStringWithDefault("wheel.width.scale.on");
		if (!string.isEmpty()) {
			ret.setWhetherWidthScaleOnMouseWheel(BooleanUtils.toBoolean(string));
		}

		// Category %6: Font Settings
		string = input.getSimplifiedStringWithDefault("font.global");
		if (!string.isEmpty()) {
			Font font = EGPSFonts.parseFont(string);
			ret.setDefaultFont(font);
		}
		string = input.getSimplifiedStringWithDefault("font.title");
		if (!string.isEmpty()) {
			Font font = EGPSFonts.parseFont(string);
			ret.setDefaultTitleFont(font);
		}
		string = input.getSimplifiedStringWithDefault("font.axis");
		if (!string.isEmpty()) {
			Font font = EGPSFonts.parseFont(string);
			ret.setAxisFont(font);
		}

		// Category %7: Layout Settings
		string = input.getSimplifiedStringWithDefault("layout.blank.space");
		if (!string.isEmpty()) {
			String[] split = EGPSStringUtil.split(string, ',');
			int top = Integer.parseInt(split[0]);
			int left = Integer.parseInt(split[1]);
			int bottom = Integer.parseInt(split[2]);
			int right = Integer.parseInt(split[3]);
			BlankArea eggsInsets = new BlankArea(top, left, bottom, right);
			ret.setBlank_space(eggsInsets);
		}

		// Category %8: Advanced
		string = input.getSimplifiedStringWithDefault("advanced.node.visual.config");
		if (!Strings.isNullOrEmpty(string)) {
			ret.setNodeVisualAnnotationConfigFilePath(string);
		}

		return ret;
	}

}
