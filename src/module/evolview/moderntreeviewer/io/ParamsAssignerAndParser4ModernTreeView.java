package module.evolview.moderntreeviewer.io;

import com.google.common.base.Strings;
import java.awt.Font;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.utils.common.util.EGPSFonts;
import graphic.engine.guicalculator.BlankArea;
import module.evoltreio.ParamsAssignerAndParser4PhyloTree;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import org.apache.commons.lang3.BooleanUtils;
import utils.string.EGPSStringUtil;


public class ParamsAssignerAndParser4ModernTreeView extends ParamsAssignerAndParser4PhyloTree {

	public ParamsAssignerAndParser4ModernTreeView() {
		super();

        addKeyValueEntryBean("%1", "Tree display parameters", "");
        for (var setting : module.evolview.moderntreeviewer.para.TreeSettings.voiceSettings()) {
            addKeyValueEntryBean(setting.key(), setting.value(), setting.comment());
        }

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
		string = input.getSimplifiedStringWithDefault("layout.initial");
		if (!string.isEmpty()) {
			TreeLayout layout = parseTreeLayout(string);
			ret.setInitialLayout(layout);
		}
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

		// Category %7.1: Rectangular Layout
		string = input.getSimplifiedStringWithDefault("layout.rectangular.curvature");
		if (!string.isEmpty()) {
			ret.setRectangularCurvature(Integer.parseInt(string));
		}

		// Category %7.2: Circular Layout
		string = input.getSimplifiedStringWithDefault("layout.circular.start.degree");
		if (!string.isEmpty()) {
			ret.setCircularStartDegree(Integer.parseInt(string));
		}
		string = input.getSimplifiedStringWithDefault("layout.circular.extent.degree");
		if (!string.isEmpty()) {
			ret.setCircularExtentDegree(Integer.parseInt(string));
		}
		string = input.getSimplifiedStringWithDefault("layout.circular.inner.radius");
		if (!string.isEmpty()) {
			ret.setCircularInnerRadius(Integer.parseInt(string));
		}

		// Category %7.3: Spiral Layout
		string = input.getSimplifiedStringWithDefault("layout.spiral.extent.degree");
		if (!string.isEmpty()) {
			ret.setSpiralExtentDegree(Integer.parseInt(string));
		}
		string = input.getSimplifiedStringWithDefault("layout.spiral.gap.factor");
		if (!string.isEmpty()) {
			ret.setSpiralGapFactor(Integer.parseInt(string));
		}
		string = input.getSimplifiedStringWithDefault("layout.spiral.beta.factor");
		if (!string.isEmpty()) {
			ret.setSpiralBetaFactor(Integer.parseInt(string));
		}

		// Category %7.4: Slant Layout
		string = input.getSimplifiedStringWithDefault("layout.slant.tree.width");
		if (!string.isEmpty()) {
			ret.setSlantTreeWidth(Integer.parseInt(string));
		}
		string = input.getSimplifiedStringWithDefault("layout.slant.left.margin");
		if (!string.isEmpty()) {
			ret.setSlantLeftMargin(Integer.parseInt(string));
		}
		string = input.getSimplifiedStringWithDefault("layout.slant.rotation");
		if (!string.isEmpty()) {
			ret.setSlantRotation(Integer.parseInt(string));
		}

		// Category %7.5: Radial Layout
		string = input.getSimplifiedStringWithDefault("layout.radial.rotation");
		if (!string.isEmpty()) {
			ret.setRadialRotation(Integer.parseInt(string));
		}

		// Category %8: Advanced
		string = input.getSimplifiedStringWithDefault("advanced.node.visual.config");
		if (!Strings.isNullOrEmpty(string)) {
			ret.setNodeVisualAnnotationConfigFilePath(string);
		}

		return ret;
	}

	/**
	 * Parse tree layout from string value.
	 * Supports both exact TreeLayout enum names and friendly names.
	 */
	private TreeLayout parseTreeLayout(String value) {
		String upper = value.toUpperCase().trim();

		// First try exact enum match
		try {
			return TreeLayout.valueOf(upper);
		} catch (IllegalArgumentException e) {
			// Try friendly name mapping
			switch (upper) {
				case "RECTANGULAR":
					return TreeLayout.RECT_PHYLO_LEFT;
				case "CIRCULAR":
					return TreeLayout.OUTER_CIR_PHYLO;
				case "SPIRAL":
					return TreeLayout.SPRIAL_ALPHA_PHYLO;
				case "SLANT":
					return TreeLayout.SLOPE_CLADO_ALIGNED_LEFT;
				case "RADIAL":
					return TreeLayout.RADIAL_PHYLO;
				default:
					return TreeLayout.RECT_PHYLO_LEFT;
			}
		}
	}

}
