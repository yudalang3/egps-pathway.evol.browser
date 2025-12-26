package module.evolview.moderntreeviewer.io;

import egps2.UnifiedAccessPoint;
import graphic.engine.guicalculator.BlankArea;
import module.evoltreio.EvolTreeImportInfoBean;

import java.awt.*;

public class MTVImportInforBean extends EvolTreeImportInfoBean {

	// Layout settings
	protected BlankArea blank_space;

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

}
