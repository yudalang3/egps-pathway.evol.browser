package module.evolview.pathwaybrowser;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.swing.JColorChooser;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.datetime.LocalDateHandler;
import egps2.UnifiedAccessPoint;
import egps2.frame.MyFrame;
import evoltree.struct.util.EvolNodeUtil;
import module.evolview.pathwaybrowser.gui.ControlPanelContainer;
import module.evolview.pathwaybrowser.gui.CtrlTreeLayoutPanel;
import module.evolview.pathwaybrowser.gui.CtrlTreeOperationPanelByMiglayout;
import module.evolview.pathwaybrowser.gui.TreeLayoutSwitcher;
import module.evolview.pathwaybrowser.gui.render.CustomizedSaveBean;
import module.evolview.pathwaybrowser.PathwayFamilyMainFace;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;
import module.evolview.phylotree.visualization.annotation.DrawPropInternalNode2LeafAnno;
import module.evolview.phylotree.visualization.annotation.DrawPropInternalNodeInsituAnno;
import module.evolview.phylotree.visualization.annotation.DrawPropLeafNameAnno;
import module.evolview.phylotree.visualization.annotation.DrawPropOutterSidewardAnno;
import module.evolview.model.enums.BranchLengthType;
import module.evolview.model.enums.ColorScheme;
import module.evolview.phylotree.visualization.annotation.AnnotationsProperties;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.graphics.struct.ShowLeafPropertiesInfo;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;

/**
 * PathwayBrowser Controller - Independent from GeneFamilyController
 * Manages tree visualization and interaction for Pathway Browser
 */
public class PathwayBrowserController {
    private static final Logger log = LoggerFactory.getLogger(PathwayBrowserController.class);

    final PathwayFamilyMainFace main;

    protected PhylogeneticTreePanel globalPhylogeneticTreePanel;
    protected TreeLayoutProperties treeLayoutProperties;

    protected final Font globalFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();
    protected final Font titleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();

    public static final DecimalFormat HUMAN_DECIMAL_INTEGER_FORMAT = new DecimalFormat(",###,###");
    public static final DateTimeFormatter HUMAN_READABLE_FORMAT = DateTimeFormatter.ofPattern("MMMM dd, yyyy",
            Locale.ENGLISH);

    private LocalDateHandler dateHandler = new LocalDateHandler();

    private TreeLayoutSwitcher toplayoutSwither;
    private ControlPanelContainer leftControlPanelContainner;

    private final CustomizedSaveBean customizedSaveBean = new CustomizedSaveBean();

    public GraphicsNode cloneOriginalRootNode;
    private GraphicsNode wholeDataRoot;

    protected MyFrame instanceFrame = UnifiedAccessPoint.getInstanceFrame();

    public Font getGlobalFont() {
        return globalFont;
    }

    public Font getTitleFont() {
        return titleFont;
    }

    public PathwayBrowserController(PathwayFamilyMainFace main) {
        this.main = main;
    }

    public PhylogeneticTreePanel getGlobalPhylogeneticTreePanel() {
        return globalPhylogeneticTreePanel;
    }

    public void setGlobalPhylogeneticTreePanel(PhylogeneticTreePanel phylogeneticTreePanel) {
        this.globalPhylogeneticTreePanel = phylogeneticTreePanel;
    }

    public PhylogeneticTreePanel getSelectedPhylogeneticTreePanel() {
        return main.getSelectedPhylogeneticTreePanel();
    }

    public TreeLayoutProperties getTreeLayoutProperties() {
        return treeLayoutProperties;
    }

    public void setTreeLayoutProperties(TreeLayoutProperties treeLayoutProperties) {
        this.treeLayoutProperties = treeLayoutProperties;
    }

    public PathwayFamilyMainFace getMain() {
        return main;
    }

    public void reCalculateTreeLayoutAndRefreshViewPort() {
        List<PhylogeneticTreePanel> list = main.getExistedPhylogeneticTreePanels();
        for (PhylogeneticTreePanel phylogeneticTreePanel : list) {
            phylogeneticTreePanel.updatePhylogeneticTreePanel4fitFrame();
        }
    }

    /**
     * This is for mouse wheel move event
     */
    public void continuouslyZoomInOrOut(int heightChange, int widthChange, Point point) {
        PhylogeneticTreePanel selectedPhylogeneticTreePanel = getSelectedPhylogeneticTreePanel();
        if (selectedPhylogeneticTreePanel != null) {
            selectedPhylogeneticTreePanel.continuouslyZoomInOrOut(heightChange, widthChange, point);
            refreshPhylogeneticTree();
        }
    }

    /**
     * Reset zoom layout panel properties
     */
    public void resetZoomLayoutPanel(int newHeight, int newWidth) {
        getTreeLayoutPanel().resetZoomLayoutPanel(newHeight, newWidth);
    }

    public void refreshPhylogeneticTree() {
        SwingUtilities.invokeLater(() -> {
            PhylogeneticTreePanel panel = getSelectedPhylogeneticTreePanel();
            if (panel != null) {
                panel.refreshViewPort();
            }
        });
    }

    public void weakRefreshPhylogeneticTree() {
        SwingUtilities.invokeLater(() -> {
            PhylogeneticTreePanel panel = getSelectedPhylogeneticTreePanel();
            if (panel != null) {
                panel.weakRefreshViewPort();
            }
        });
    }

    public void clearAllCollapseNodes() {
        if (treeLayoutProperties != null) {
            treeLayoutProperties.clearCollapseProproties();

            TreeOperationUtil.recursiveIterateTreeIF(getTreeLayoutProperties().getOriginalRootNode(), node -> {
                node.setCollapse(false);
            });

            List<PhylogeneticTreePanel> existedPhylogeneticTreePanels = main.getExistedPhylogeneticTreePanels();
            for (PhylogeneticTreePanel phylogeneticTreePanel : existedPhylogeneticTreePanels) {
                GraphicsNode rootNode = phylogeneticTreePanel.getRootNode();
                TreeOperationUtil.recursiveIterateTreeIF(rootNode, node -> {
                    node.setCollapse(false);
                });
            }

            reCalculateTreeLayoutAndRefreshViewPort();
        }
    }

    public void changeColorForSelectedNodesInTreePanel() {
        if (treeLayoutProperties == null) return;

        List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
        GraphicsNode selectedNode = null;
        if (selectedNodes.size() > 0) {
            selectedNode = selectedNodes.get(0);
        } else {
            selectedNode = treeLayoutProperties.getOriginalRootNode();
        }

        Color lineColor = selectedNode.getDrawUnit().getLineColor();
        Color color = JColorChooser.showDialog(instanceFrame, "Recolor", lineColor);

        if (color == null) {
            return;
        }

        TreeOperationUtil.recursiveIterateTreeIF(selectedNode, node -> {
            node.getDrawUnit().setLineColor(color);
        });

        refreshPhylogeneticTree();
    }

    public void increaseLineThinknessForSelectedNodesInTreePanel(boolean onlyLeaf) {
        if (treeLayoutProperties == null) return;

        List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
        List<GraphicsNode> node2operate = new LinkedList<>();

        if (selectedNodes.size() > 0) {
            node2operate.addAll(selectedNodes);
        } else {
            GraphicsNode root = treeLayoutProperties.getOriginalRootNode();
            node2operate.add(root);
        }

        for (GraphicsNode graphicsNode : node2operate) {
            GraphicsNode determinedNode = onlyLeaf ? (GraphicsNode) EvolNodeUtil.getFirstLeaf(graphicsNode) : graphicsNode;
            final float thinkness = determinedNode.getDrawUnit().getStrokeSize() + 1;
            TreeOperationUtil.recursiveIterateTreeIF(graphicsNode, node -> {
                if (onlyLeaf) {
                    if (node.getChildCount() == 0) {
                        node.getDrawUnit().setStrokeSize(thinkness);
                    }
                } else {
                    node.getDrawUnit().setStrokeSize(thinkness);
                }
            });
        }
        refreshPhylogeneticTree();
    }

    public void decreaseLineThinknessForSelectedNodesInTreePanel(boolean onlyLeaf) {
        if (treeLayoutProperties == null) return;

        List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
        List<GraphicsNode> node2operate = new LinkedList<>();

        if (selectedNodes.size() > 0) {
            node2operate.addAll(selectedNodes);
        } else {
            GraphicsNode root = treeLayoutProperties.getOriginalRootNode();
            node2operate.add(root);
        }

        for (GraphicsNode graphicsNode : node2operate) {
            GraphicsNode determinedNode = onlyLeaf ? (GraphicsNode) EvolNodeUtil.getFirstLeaf(graphicsNode) : graphicsNode;
            final float thinkness = determinedNode.getDrawUnit().getStrokeSize() - 1;
            if (thinkness < 0) {
                break;
            }
            TreeOperationUtil.recursiveIterateTreeIF(graphicsNode, node -> {
                if (onlyLeaf) {
                    if (node.getChildCount() == 0) {
                        node.getDrawUnit().setStrokeSize(thinkness);
                    }
                } else {
                    node.getDrawUnit().setStrokeSize(thinkness);
                }
            });
        }
        refreshPhylogeneticTree();
    }

    public void increaseSizeForSelectedNodesInTreePanel(boolean onlyLeaf) {
        if (treeLayoutProperties == null) return;

        List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
        List<GraphicsNode> node2operate = new LinkedList<>();

        if (selectedNodes.size() > 0) {
            node2operate.addAll(selectedNodes);
        } else {
            GraphicsNode root = treeLayoutProperties.getOriginalRootNode();
            node2operate.add(root);
        }

        for (GraphicsNode graphicsNode : node2operate) {
            GraphicsNode determinedNode = onlyLeaf ? (GraphicsNode) EvolNodeUtil.getFirstLeaf(graphicsNode) : graphicsNode;
            final int circleRadius = determinedNode.getDrawUnit().getCircleRadius() + 1;
            if (circleRadius < 1) {
                break;
            }
            TreeOperationUtil.recursiveIterateTreeIF(graphicsNode, node -> {
                if (onlyLeaf) {
                    if (node.getChildCount() == 0) {
                        node.getDrawUnit().setCircleRadius(circleRadius);
                    }
                } else {
                    node.getDrawUnit().setCircleRadius(circleRadius);
                }
            });
        }
        refreshPhylogeneticTree();
    }

    public void decreaseSizeForSelectedNodesInTreePanel(boolean onlyLeaf) {
        if (treeLayoutProperties == null) return;

        List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
        List<GraphicsNode> node2operate = new LinkedList<>();

        if (selectedNodes.size() > 0) {
            node2operate.addAll(selectedNodes);
        } else {
            GraphicsNode root = treeLayoutProperties.getOriginalRootNode();
            node2operate.add(root);
        }

        for (GraphicsNode graphicsNode : node2operate) {
            GraphicsNode determinedNode = onlyLeaf ? (GraphicsNode) EvolNodeUtil.getFirstLeaf(graphicsNode) : graphicsNode;
            final int circleRadius = determinedNode.getDrawUnit().getCircleRadius() - 1;
            if (circleRadius < 0) {
                break;
            }
            TreeOperationUtil.recursiveIterateTreeIF(graphicsNode, node -> {
                if (onlyLeaf) {
                    if (node.getChildCount() == 0) {
                        node.getDrawUnit().setCircleRadius(circleRadius);
                    }
                } else {
                    node.getDrawUnit().setCircleRadius(circleRadius);
                }
            });
        }
        refreshPhylogeneticTree();
    }

    public void searchForSpecificLeavesInTreePanel(String text) {
        if (treeLayoutProperties == null) return;

        List<GraphicsNode> leaves = getTreeLayoutProperties().getLeaves();
        for (GraphicsNode graphicsNode : leaves) {
            if (graphicsNode.getName().equalsIgnoreCase(text)) {
                graphicsNode.getDrawUnit().setBranchSelected(true);
                break;
            }
        }
        refreshPhylogeneticTree();
    }

    public void clearSearchedNode() {
        if (treeLayoutProperties == null) return;

        TreeOperationUtil.recursiveIterateTreeIF(treeLayoutProperties.getOriginalRootNode(), node -> {
            node.getDrawUnit().setNodeSelected(false);
        });
        refreshPhylogeneticTree();
    }

    public void showLeaveLabel(boolean selected) {
        if (treeLayoutProperties == null) return;

        ShowLeafPropertiesInfo showLeafPropertiesInfo = treeLayoutProperties.getShowLeafPropertiesInfo();
        List<GraphicsNode> leaves = treeLayoutProperties.getLeaves();
        for (GraphicsNode graphicsNode : leaves) {
            graphicsNode.getDrawUnit().setDrawName(selected);
        }

        if (selected) {
            showLeafPropertiesInfo.setNeedChange4showLabel(true);
            showLeafPropertiesInfo.setNeedChange4hideLabel(false);
        } else {
            showLeafPropertiesInfo.setNeedChange4showLabel(false);
            showLeafPropertiesInfo.setNeedChange4hideLabel(true);
        }
        showLeafPropertiesInfo.setShowLeafLabel(selected);
    }

    public void setBranchLengthType(BranchLengthType branchLengthType) {
        // Implementation can be added if needed for pathway browser
        log.debug("setBranchLengthType called with: {}", branchLengthType);
    }

    public void showBranchLabel(boolean isSelect, boolean highlightBacMuts, int labelIndex) {
        if (treeLayoutProperties != null) {
            getTreeLayoutProperties().setShowBranchLabel(isSelect, highlightBacMuts, labelIndex);
            reCalculateTreeLayoutAndRefreshViewPort();
        }
    }

    public GraphicsNode getWholeDataRoot() {
        return wholeDataRoot;
    }

    public void setWholeDataRoot(GraphicsNode wholeDataRoot) {
        this.wholeDataRoot = wholeDataRoot;
    }

    public void letGlobalTreePanelChangeRoot(GraphicsNode newRoot) {
        if (treeLayoutProperties == null || globalPhylogeneticTreePanel == null) return;

        getTreeLayoutProperties().resetNewRootNode(newRoot);
        globalPhylogeneticTreePanel.resetNewRoot(newRoot);

        AnnotationsProperties mainAnnotationsProperties = getGlobalPhylogeneticTreePanel()
                .getMainAnnotationsProperties();
        mainAnnotationsProperties.setShouldNotConfigurateIfNoGraphicsNode();
        TreeOperationUtil.recursiveIterateTreeIF(newRoot, node -> {
            List<DrawPropInternalNode2LeafAnno> internalNode2LeafAnnos = mainAnnotationsProperties
                    .getInternalNode2LeafAnnos();
            for (DrawPropInternalNode2LeafAnno tt : internalNode2LeafAnnos) {
                tt.substituteNodeIfMeetNodeID(node);
            }
            List<DrawPropOutterSidewardAnno> outterSidewardAnnos = mainAnnotationsProperties.getOutterSidewardAnnos();
            for (DrawPropOutterSidewardAnno tt : outterSidewardAnnos) {
                tt.substituteNodeIfMeetNodeID(node);
            }
            List<DrawPropInternalNodeInsituAnno> internalNode2LeafInsituAnnos = mainAnnotationsProperties
                    .getInternalNode2LeafInsituAnnos();
            for (DrawPropInternalNodeInsituAnno tt : internalNode2LeafInsituAnnos) {
                tt.substituteNodeIfMeetNodeID(node);
            }
            List<DrawPropLeafNameAnno> leafNameAnnos = mainAnnotationsProperties.getLeafNameAnnos();
            for (DrawPropLeafNameAnno tt : leafNameAnnos) {
                tt.substituteNodeIfMeetNodeID(node);
            }
        });
        mainAnnotationsProperties.assignNullIfShouldNotConfig4all();

        showGlobalTreePanel();
        globalPhylogeneticTreePanel.updatePhylogeneticTreePanel();
    }

    public void changeColorScheme(ColorScheme colorScheme) {
        if (treeLayoutProperties != null) {
            List<PhylogeneticTreePanel> panels = main.getExistedPhylogeneticTreePanels();
            for (PhylogeneticTreePanel panel : panels) {
                assignNodeColor4treePanel(colorScheme, panel.getRootNode());
            }
            reCalculateTreeLayoutAndRefreshViewPort();
        }
    }

    public void assignNodeColor4treePanel(ColorScheme colorScheme, GraphicsNode root) {
        switch (colorScheme) {
            case NO_COLOR:
                TreeOperationUtil.recursiveIterateTreeIF(root, node -> {
                    if (node.getChildCount() == 0) {
                        node.getDrawUnit().setLineColor(Color.black);
                    } else {
                        node.getDrawUnit().setLineColor(Color.lightGray);
                    }
                });
                break;
            case CUSTOMIZED:
                log.warn("CUSTOMIZED color scheme not implemented yet");
                break;
            default:
                break;
        }
    }

    public void setTreeLayoutSwither(TreeLayoutSwitcher toplayoutSwither2) {
        this.toplayoutSwither = toplayoutSwither2;
    }

    public void enableTreeLayoutSwitherButtons(boolean enable) {
        if (toplayoutSwither != null) {
            SwingUtilities.invokeLater(() -> {
                toplayoutSwither.enableTreeLayoutSwitcherButtons(enable);
            });
        }
    }

    public void setLeftControlPanelContainner(ControlPanelContainer leftControlPanelContainner) {
        this.leftControlPanelContainner = leftControlPanelContainner;
    }

    public ControlPanelContainer getLeftControlPanelContainner() {
        return leftControlPanelContainner;
    }

    public CtrlTreeOperationPanelByMiglayout getLeftOparetionPanel() {
        return leftControlPanelContainner != null ? leftControlPanelContainner.getLeftOparetionPanel() : null;
    }

    public CtrlTreeLayoutPanel getTreeLayoutPanel() {
        return leftControlPanelContainner != null ? leftControlPanelContainner.getTreeLayoutPanel() : null;
    }

    public DateTimeFormatter getHumanReadableDataFormat() {
        return HUMAN_READABLE_FORMAT;
    }

    public String getMouthNameAbbrAndYearMouthDay(String str) {
        return dateHandler.getMonthNameAbbrAndYearMonthDay(str);
    }

    public String getMouthNameAbbrAndYearMouth(String str) {
        return dateHandler.getMonthNameAbbrAndYearMonth(str);
    }

    public String getMouthNameAbbr(String name) {
        return dateHandler.getMonthNameAbbr(name);
    }

    public void showGlobalTreePanel() {
        // Implementation can be added if needed
        log.debug("showGlobalTreePanel called");
    }

    public CustomizedSaveBean getCustomizedSaveBean() {
        return customizedSaveBean;
    }

    public void recoverAllGraphicsEffects() {
        if (treeLayoutProperties != null) {
            TreeOperationUtil.recursiveIterateTreeIF(getTreeLayoutProperties().getOriginalRootNode(), node -> {
                node.getDrawUnit().recover2PrimaryStates();
            });

            getTreeLayoutProperties().recover2defaultFont();
            refreshPhylogeneticTree();
        }
    }

    public void fireLayoutChanged() {
        // Hook for layout change events
        log.debug("fireLayoutChanged called");
    }
}
