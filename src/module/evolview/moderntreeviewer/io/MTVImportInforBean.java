package module.evolview.moderntreeviewer.io;

import egps2.UnifiedAccessPoint;
import graphic.engine.guicalculator.BlankArea;
import module.evoltreio.EvolTreeImportInfoBean;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;

import java.awt.*;

public class MTVImportInforBean extends EvolTreeImportInfoBean {

	// Layout settings
	protected BlankArea blank_space = new BlankArea(20, 40, 80, 40);  // default: top,left,bottom,right
	protected TreeLayout initialLayout = TreeLayout.RECT_PHYLO_LEFT;

	// Slant layout parameters
	protected int slantTreeWidth = 100;        // 0-100, percentage
	protected int slantLeftMargin = 20;        // 0-100, percentage
	protected int slantRotation = 0;           // 0, 90, 180, 270

	// Radial layout parameters
	protected int radialRotation = 0;          // 0-360

	// Rectangular layout parameters
	protected int rectangularCurvature = 0;    // 0-100

	// Circular layout parameters
	protected int circularStartDegree = 285;   // 0-360
	protected int circularExtentDegree = 360;  // 0-360
	protected int circularInnerRadius = 50;    // 0-200, for Inner Cladogram

	// Spiral layout parameters
	protected int spiralExtentDegree = 720;    // 0-10000
	protected int spiralGapFactor = 10;        // 0-50
	protected int spiralBetaFactor = 100;      // 100-500, unit is 0.01

	// Tree Information (%1)
	protected boolean showScaleBar = false;
	protected boolean showAxisBar = true;
	protected boolean showTitle = true;
	protected boolean showWidthAndHeightString = false;
	protected String titleString;
	protected String branchLengthUnit = "";
	protected boolean needReverseAxisBar = false;

	// Leaf Node (%2)
	protected boolean showLeafLabel = false;
	protected boolean shouldLeafNameRightAlign = false;

	// Inner Node (%3)
	protected boolean showInnerNodeLabel = false;
	protected boolean showBootstrap = false;
	protected boolean showBranchLength = false;

	// Root Settings (%4)
	protected boolean showRoot = false;
	protected double rootTipLength = 10;

	// Mouse Wheel (%5)
	protected boolean whetherWidthScaleOnMouseWheel = true;
	protected boolean whetherHeightScaleOnMouseWheel = true;

	// Font Settings (%6)
	protected Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont().deriveFont(14f);
	protected Font defaultTitleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();
	protected Font axisFont = null;

	// Advanced (%8)
	private String nodeVisualAnnotationConfigFilePath;
	
	public boolean isShouldLeafNameRightAlign() {
		return shouldLeafNameRightAlign;
	}

	public void setShouldLeafNameRightAlign(boolean shouldLeafNameRightAlign) {
		this.shouldLeafNameRightAlign = shouldLeafNameRightAlign;
	}
	
	
	public void setBlank_space(BlankArea egpsInsets) {
		this.blank_space = egpsInsets;
	}

	public BlankArea getBlank_space() {
		return blank_space;
	}

	public String getTitleString() {
		return titleString;
	}

	public void setTitleString(String titleString) {
		this.titleString = titleString;
	}

	public boolean isShowLeafLabel() {
		return showLeafLabel;
	}

	public void setShowLeafLabel(boolean showLeafLabel) {
		this.showLeafLabel = showLeafLabel;
	}

	public Font getDefaultFont() {
		return defaultFont;
	}

	public void setDefaultFont(Font defaultFont) {
		this.defaultFont = defaultFont;
	}

	public boolean isWhetherWidthScaleOnMouseWheel() {
		return whetherWidthScaleOnMouseWheel;
	}

	public void setWhetherWidthScaleOnMouseWheel(boolean whetherWidthScaleOnMouseWheel) {
		this.whetherWidthScaleOnMouseWheel = whetherWidthScaleOnMouseWheel;
	}

	public boolean isWhetherHeightScaleOnMouseWheel() {
		return whetherHeightScaleOnMouseWheel;
	}

	public void setWhetherHeightScaleOnMouseWheel(boolean whetherHeightScaleOnMouseWheel) {
		this.whetherHeightScaleOnMouseWheel = whetherHeightScaleOnMouseWheel;
	}
	
	public boolean isNeedReverseAxisBar() {
		return needReverseAxisBar;
	}

	public void setNeedReverseAxisBar(boolean needReverseAxisBar) {
		this.needReverseAxisBar = needReverseAxisBar;
	}

	public void setBranchLengthUnit(String string) {
		branchLengthUnit = string;
	}

	public String getBranchLengthUnit() {
		return branchLengthUnit;
	}

	public Font getDefaultTitleFont() {
		return defaultTitleFont;
	}

	public void setDefaultTitleFont(Font defaultTitleFont) {
		this.defaultTitleFont = defaultTitleFont;
	}

	public void setNodeVisualAnnotationConfigFilePath(String string) {
		this.nodeVisualAnnotationConfigFilePath = string;
	}
	
	public String getNodeVisualAnnotationConfigFilePath() {
		return nodeVisualAnnotationConfigFilePath;
	}

	// Tree Information (%1) getters/setters
	public boolean isShowScaleBar() {
		return showScaleBar;
	}

	public void setShowScaleBar(boolean showScaleBar) {
		this.showScaleBar = showScaleBar;
	}

	public boolean isShowAxisBar() {
		return showAxisBar;
	}

	public void setShowAxisBar(boolean showAxisBar) {
		this.showAxisBar = showAxisBar;
	}

	public boolean isShowTitle() {
		return showTitle;
	}

	public void setShowTitle(boolean showTitle) {
		this.showTitle = showTitle;
	}

	public boolean isShowWidthAndHeightString() {
		return showWidthAndHeightString;
	}

	public void setShowWidthAndHeightString(boolean showWidthAndHeightString) {
		this.showWidthAndHeightString = showWidthAndHeightString;
	}

	// Inner Node (%3) getters/setters
	public boolean isShowInnerNodeLabel() {
		return showInnerNodeLabel;
	}

	public void setShowInnerNodeLabel(boolean showInnerNodeLabel) {
		this.showInnerNodeLabel = showInnerNodeLabel;
	}

	public boolean isShowBootstrap() {
		return showBootstrap;
	}

	public void setShowBootstrap(boolean showBootstrap) {
		this.showBootstrap = showBootstrap;
	}

	public boolean isShowBranchLength() {
		return showBranchLength;
	}

	public void setShowBranchLength(boolean showBranchLength) {
		this.showBranchLength = showBranchLength;
	}

	// Root Settings (%4) getters/setters
	public boolean isShowRoot() {
		return showRoot;
	}

	public void setShowRoot(boolean showRoot) {
		this.showRoot = showRoot;
	}

	public double getRootTipLength() {
		return rootTipLength;
	}

	public void setRootTipLength(double rootTipLength) {
		this.rootTipLength = rootTipLength;
	}

	// Font Settings (%6) getters/setters
	public Font getAxisFont() {
		return axisFont;
	}

	public void setAxisFont(Font axisFont) {
		this.axisFont = axisFont;
	}

	// Layout Settings (%7) - initial layout
	public TreeLayout getInitialLayout() {
		return initialLayout;
	}

	public void setInitialLayout(TreeLayout initialLayout) {
		this.initialLayout = initialLayout;
	}

	// Slant layout getters/setters
	public int getSlantTreeWidth() {
		return slantTreeWidth;
	}

	public void setSlantTreeWidth(int slantTreeWidth) {
		this.slantTreeWidth = slantTreeWidth;
	}

	public int getSlantLeftMargin() {
		return slantLeftMargin;
	}

	public void setSlantLeftMargin(int slantLeftMargin) {
		this.slantLeftMargin = slantLeftMargin;
	}

	public int getSlantRotation() {
		return slantRotation;
	}

	public void setSlantRotation(int slantRotation) {
		this.slantRotation = slantRotation;
	}

	// Radial layout getters/setters
	public int getRadialRotation() {
		return radialRotation;
	}

	public void setRadialRotation(int radialRotation) {
		this.radialRotation = radialRotation;
	}

	// Rectangular layout getters/setters
	public int getRectangularCurvature() {
		return rectangularCurvature;
	}

	public void setRectangularCurvature(int rectangularCurvature) {
		this.rectangularCurvature = rectangularCurvature;
	}

	// Circular layout getters/setters
	public int getCircularStartDegree() {
		return circularStartDegree;
	}

	public void setCircularStartDegree(int circularStartDegree) {
		this.circularStartDegree = circularStartDegree;
	}

	public int getCircularExtentDegree() {
		return circularExtentDegree;
	}

	public void setCircularExtentDegree(int circularExtentDegree) {
		this.circularExtentDegree = circularExtentDegree;
	}

	public int getCircularInnerRadius() {
		return circularInnerRadius;
	}

	public void setCircularInnerRadius(int circularInnerRadius) {
		this.circularInnerRadius = circularInnerRadius;
	}

	// Spiral layout getters/setters
	public int getSpiralExtentDegree() {
		return spiralExtentDegree;
	}

	public void setSpiralExtentDegree(int spiralExtentDegree) {
		this.spiralExtentDegree = spiralExtentDegree;
	}

	public int getSpiralGapFactor() {
		return spiralGapFactor;
	}

	public void setSpiralGapFactor(int spiralGapFactor) {
		this.spiralGapFactor = spiralGapFactor;
	}

	public int getSpiralBetaFactor() {
		return spiralBetaFactor;
	}

	public void setSpiralBetaFactor(int spiralBetaFactor) {
		this.spiralBetaFactor = spiralBetaFactor;
	}

}
