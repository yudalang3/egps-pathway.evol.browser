package module.evolview.moderntreeviewer.io;

import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import module.evoltreio.NewickFormatConfig;
import module.evoltreio.ParamsAssignerAndParser4PhyloTree;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.para.TreeSettings;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.apache.commons.lang3.BooleanUtils;

/** Modern Tree View owns its aliases; other tree modules retain their contract. */
public class ParamsAssignerAndParser4ModernTreeView extends ParamsAssignerAndParser4PhyloTree {
    public ParamsAssignerAndParser4ModernTreeView() {
        super();
        getRequiredParams().clear();
        addKeyValueEntryBean("%0", "Tree input", "");
        for (var setting : TreeSettings.inputSettings())
            addKeyValueEntryBean(setting.key(), setting.value(), setting.description());
        addKeyValueEntryBean("%1", "Tree display parameters", "");
        for (var setting : TreeSettings.voiceSettings())
            addKeyValueEntryBean(setting.key(), setting.value(), setting.comment());
    }

    @Override
    public MTVImportInforBean generateTreeFromKeyValue(OrganizedParameterGetter input) {
        MTVImportInforBean bean = new MTVImportInforBean();
        bean.setInput_nwk_string(TreeSettings.inputValue(input, "input.newick.text"));
        bean.setInput_nwk_path(TreeSettings.inputValue(input, "input.newick.file"));
        bean.setInput_tableLike_path(TreeSettings.inputValue(input, "input.table.file"));
        try {
            int format = Integer.parseInt(TreeSettings.inputValue(input, "input.newick.format"));
            if (!NewickFormatConfig.isValidFormat(format)) throw new IllegalArgumentException("Use a Newick format from 0 to 9");
            bean.setNwkFormat(format);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("input.newick.format: " + error.getMessage(), error);
        }
        Boolean whitespace = BooleanUtils.toBooleanObject(TreeSettings.inputValue(input, "input.newick.remove.whitespace"));
        if (whitespace == null) throw new IllegalArgumentException("input.newick.remove.whitespace: Use true, false, T or F");
        bean.setRemoveWhitespace(whitespace);
        String styles = TreeSettings.inputValue(input, "input.node.styles.file");
        if (!styles.isEmpty()) bean.setNodeVisualAnnotationConfigFilePath(styles);

        // Keep the public Bean; explicit values and validation come from the registry.
        TreeLayoutProperties p = new TreeLayoutProperties(new GraphicsNode());
        TreeSettings.apply(p, TreeSettings.defaults());
        var explicit = TreeSettings.explicitImport(input, p);
        bean.setExplicitSettings(explicit);
        TreeSettings.apply(p, explicit);
        bean.setShowScaleBar(p.isShowScaleBar());
        bean.setShowAxisBar(p.isShowAxisBar());
        bean.setShowTitle(p.isShowTitle());
        bean.setShowWidthAndHeightString(p.isShowWidthAndHeightString());
        bean.setTitleString(p.getTitleString());
        bean.setBranchLengthUnit(p.getBranchLengthUnit());
        bean.setNeedReverseAxisBar(p.isNeedReverseAxisBar());
        bean.setShowLeafLabel(p.getShowLeafPropertiesInfo().isShowLeafLabel());
        bean.setShouldLeafNameRightAlign(p.isShouldLeafNameRightAlign());
        bean.setShowInnerNodeLabel(p.getShowInnerNodePropertiesInfo().isShowInternalNodeLabel());
        bean.setShowBootstrap(p.getShowInnerNodePropertiesInfo().isShowInternalNodeBootstrap());
        bean.setShowBranchLength(p.isShowNodeBranchLength());
        bean.setShowRoot(p.isShowRoot());
        bean.setRootTipLength(p.getRootTipLength());
        bean.setWhetherHeightScaleOnMouseWheel(p.isWhetherHeightScaleOnMouseWheel());
        bean.setWhetherWidthScaleOnMouseWheel(p.isWhetherWidthScaleOnMouseWheel());
        bean.setDefaultFont(p.getGlobalFont());
        bean.setDefaultTitleFont(p.getTitleFont());
        if (!p.isAxisFontInherited()) bean.setAxisFont(p.getAxisFont());
        bean.setInitialLayout(p.getMyLayout());
        bean.setBlank_space(p.getConfiguredBlankArea());
        bean.setRectangularCurvature(p.getRectangularLayoutPropertiy().getCurvature());
        bean.setCircularStartDegree(p.getCircularLayoutPropertiy().getGlobalStartDegree());
        bean.setCircularExtentDegree(p.getCircularLayoutPropertiy().getGlobalExtendDegree());
        bean.setCircularInnerRadius(p.getCircularLayoutPropertiy().getInnerCircleRadicus());
        bean.setCircularInnerRadiusRatio(p.getCircularLayoutPropertiy().getInnerRadiusRatio());
        bean.setSpiralExtentDegree(p.getSprialLayoutPropertiy().getGlobalExtendingDegree());
        bean.setSpiralGapFactor(p.getSprialLayoutPropertiy().getGapSize());
        bean.setSpiralBetaFactor((int) Math.round(p.getSprialLayoutPropertiy().getBetaFactor() * 100));
        bean.setSlantTreeWidth((int) Math.round(p.getSlopeLayoutProperety().getLeafLocationRightMostRatio() * 100));
        bean.setSlantLeftMargin((int) Math.round(p.getSlopeLayoutProperety().getBlankMarginRatio() * 100));
        bean.setSlantRotation(p.getSlopeLayoutProperety().getSlopeLayoutRotationDeg());
        bean.setRadialRotation(p.getRadicalLayoutRotationDeg());
        return bean;
    }
}
