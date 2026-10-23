package module.evolview.gfamily.work.gui.tree;

import egps2.UnifiedAccessPoint;
import egps2.builtin.modules.IconObtainer;
import egps2.frame.MainFrameProperties;
import egps2.frame.gui.dialog.DialogMoreInfoGenerator;
import egps2.panels.dialog.EGPSFileChooser;
import egps2.panels.dialog.SwingDialog;
import egps2.utils.common.model.datatransfer.Tuple;
import evoltree.struct.util.EvolNodeUtil;
import evoltree.txtdisplay.TreeDrawUnit;
import module.evoltre.mutation.IMutation4Rec;
import module.evolview.gfamily.work.gui.dialog.NodeAnnotationDialogContainer;
import module.evolview.model.enums.BranchLengthType;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.IndependentModuleLoader;
import module.evolview.phylotree.visualization.graphics.struct.CollapseProperty;
import module.evolview.phylotree.visualization.layout.BaseLayout;
import module.evolview.phylotree.visualization.layout.LeafLabelStateInitializer;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class TreePopupMenu {
	private static final Logger log = LoggerFactory.getLogger(TreePopupMenu.class);

	private TreeLayoutProperties treeLayoutProperties;

	protected GraphicsNode currNode = null;
	protected final static DecimalFormat deciFor = new DecimalFormat("#.######");

	protected JPopupMenu popInternalNodeRightClick;
	protected JPopupMenu popLeafRightClick;
	protected JPopupMenu blinkAreaPopupMenu;

	private boolean debug = false;

	protected StringBuilder sBuilder = new StringBuilder();

	Function<IMutation4Rec, String> printSimpleMutationInfo = mutation -> {
		String origin = mutation.getAncestralState();
		int position = mutation.getPosition();
		String destination = mutation.getDerivedState();
		String newOrigin = origin;
		if (origin.length() > 3) {
			newOrigin = origin.substring(0, 1) + "..." + origin.substring(origin.length() - 1);
		}
		String newDestination = destination;
		if (destination.length() > 3) {
			newDestination = destination.substring(0, 1) + "..." + destination.substring(destination.length() - 1);
		}
		return newOrigin + "" + position + "" + newDestination;

	};
	Function<IMutation4Rec, String> printAllMutationInfo = mutation -> {
		return mutation.toString();
	};
	protected JMenuItem showInRightByCurrentRoot;
	protected JMenuItem showInRightByOriginalRoot;
	private JMenuItem clearAnnotatonJmenu4internalNode;

	protected final String BUTTOM_HTML_STRING = "<br><i>Right click or left click with alt to see more information<br>"
			+ "Right click and open new tab to see clade</i>";
	private JMenuItem clearAnnotaton4blinkArea;
	private JMenuItem clearCollapseNode;
	private JMenuItem refreshPhylogeneticTreePanel;
	private JMenuItem recover2defaultGraphicEffects;
	private JMenuItem removeAllSelections;
	private JMenuItem removeCustomizedColors;
	protected JMenuItem selectDescendants;


	private PhylogeneticTreePanel phylogeneticTreePanel;

	public TreePopupMenu(PhylogeneticTreePanel phylogeneticTreePanel) {
		this.phylogeneticTreePanel = phylogeneticTreePanel;

	}

	public TreeLayoutProperties getTreeLayoutProperties() {
		if (treeLayoutProperties == null) {
			treeLayoutProperties = phylogeneticTreePanel.getLayoutProperties();
		}
		return treeLayoutProperties;
	}

	private JMenuItem detachNodeJMenuItem() {
		GraphicsNode target = currNode;
		JMenuItem item = new JMenuItem("Detach node");
		item.setToolTipText("Confirm removal of this subtree. Keep surviving unary parents and branch lengths.");
		item.setEnabled(phylogeneticTreePanel.canRemoveSubtree(target));
		item.addActionListener(e -> phylogeneticTreePanel.removeSubtree(target, false));
		return item;
	}

	private JMenuItem getDisplayNodeNameMenuItem() {
		GraphicsNode target = currNode;
		JMenuItem item = new JMenuItem("Display/Hide node name");
		item.setEnabled(phylogeneticTreePanel.canEditTreeNode(target));
		item.setToolTipText("Toggle this node's label setting. The global label setting also applies.");
		item.addActionListener(e -> {
			if (!phylogeneticTreePanel.canEditTreeNode(target)) return;
			TreeDrawUnit unit = target.getDrawUnit();
			unit.setDrawName(!unit.isDrawName());
			phylogeneticTreePanel.recalculatePreservingZoom();
		});
		return item;
	}

	public void displayMoreInformation4leaf(GraphicsNode selectedNode) {
		List<String> lists = new ArrayList<>();
		lists.add("Name: " + Objects.toString(selectedNode.getName(), ""));
		lists.add("Real branch length: " + selectedNode.getRealBranchLength());
		if (Double.compare(selectedNode.getLength(), selectedNode.getRealBranchLength()) != 0)
			lists.add("Displayed branch length: " + selectedNode.getLength());

		DialogMoreInfoGenerator dialog = new DialogMoreInfoGenerator(lists);
		dialog.setTitle("Leaf node information");
		dialog.setVisible(true);
	}

	protected String getTitle(GraphicsNode currNode2) {

		String str = null;
		if (currNode2.getChildCount() == 0) {
			str = currNode2.getName();
		} else {
			str = currNode2.getCGBID();
		}
		return "$axis:title=".concat(str);
	}

	private JMenuItem getRecolorJMenuItem4all() {
		GraphicsNode target = currNode;
		JMenuItem item = new JMenuItem("Recolor...");
		item.setIcon(IconObtainer.get("color.png"));
		item.setToolTipText("Recolor this node; optionally include all descendants.");
		item.addActionListener(e -> {
			if (!phylogeneticTreePanel.containsTreeNode(target)) return;
			Color color = chooseBranchColor(target.getDrawUnit().getLineColor());
			if (color == null) return;
			int choice = target.getChildCount() == 0 ? JOptionPane.NO_OPTION : chooseRecolorScope();
			if (choice != JOptionPane.YES_OPTION && choice != JOptionPane.NO_OPTION) return;
			if (!phylogeneticTreePanel.containsTreeNode(target)) return;
			for (GraphicsNode node : choice == JOptionPane.YES_OPTION
					? PhylogeneticTreePanel.subtreeNodes(target) : List.of(target)) node.getDrawUnit().setLineColor(color);
			phylogeneticTreePanel.notifyTreeViewStateChanged();
			phylogeneticTreePanel.refreshGraphicSelectionControls();
			phylogeneticTreePanel.refreshViewPort();
		});
		return item;
	}

	protected Color chooseBranchColor(Color initial) {
		return JColorChooser.showDialog(phylogeneticTreePanel, "Choose Color", initial);
	}

	protected int chooseRecolorScope() {
		return JOptionPane.showConfirmDialog(phylogeneticTreePanel, "Apply to all descendant nodes?", "Recolor",
				JOptionPane.YES_NO_CANCEL_OPTION);
	}

	private JMenuItem getZoomInThisNodeJMenuItem() {
		GraphicsNode target = currNode;
		JMenuItem item = new JMenuItem("Zoom to see node");
		item.setIcon(IconObtainer.get("zoomIn2.png"));
		item.setToolTipText("Zoom around this node and center it in the viewport.");
		item.setEnabled(phylogeneticTreePanel.canEditTreeNode(target));
		item.addActionListener(e -> {
			if (!phylogeneticTreePanel.canEditTreeNode(target)) return;
			phylogeneticTreePanel.continuouslyZoomInOrOut(2500, 2500,
					new Point((int) target.getXSelf(), (int) target.getYSelf()));
			SwingUtilities.invokeLater(() -> {
				if (phylogeneticTreePanel.containsTreeNode(target)) phylogeneticTreePanel.scrollNodeToCenter(target);
			});
		});
		return item;
	}

	/**
	 * 内节点右击时显示的菜单栏
	 * 
	 * @return
	 */
	protected JPopupMenu getPopInternalNodeRightClick() {

		GraphicsNode target = currNode;
		popInternalNodeRightClick = new JPopupMenu();

		JMenuItem showInRightByCurrentRoot = getShowInRightByCurrentRootJMenuItem();


		JMenuItem annotation = new JMenuItem("Annotation...");
		annotation.setToolTipText("Annotate this clade.");
		annotation.setIcon(IconObtainer.get("Annotation.png"));
		annotation.addActionListener(e -> {
			if (!phylogeneticTreePanel.canEditTreeNode(target)) return;
			NodeAnnotationDialogContainer nodeAnnotationDialog = new NodeAnnotationDialogContainer(
					phylogeneticTreePanel, target);

			nodeAnnotationDialog.setVisible(true);
		});

		JMenuItem ladderizeUp = new JMenuItem("Ladderize up");
		ladderizeUp.setIcon(IconObtainer.get("ladderizeUp.png"));
		ladderizeUp.setToolTipText("Ladderize up this clade.");
		ladderizeUp.addActionListener(e -> {
			if (!phylogeneticTreePanel.canEditTreeNode(target)) return;
			EvolNodeUtil.initializeSize(target);
			EvolNodeUtil.ladderizeNodeAccording2sizeAndLength(target, true);
			phylogeneticTreePanel.completeTreeStructureChange();
		});

		JMenuItem ladderizeDown = new JMenuItem("Ladderize down");
		ladderizeDown.setIcon(IconObtainer.get("ladderizeDown.png"));
		ladderizeDown.setToolTipText("Ladderize down this clade.");
		ladderizeDown.addActionListener(e -> {
			if (!phylogeneticTreePanel.canEditTreeNode(target)) return;
			EvolNodeUtil.initializeSize(target);
			EvolNodeUtil.ladderizeNodeAccording2sizeAndLength(target, false);
			phylogeneticTreePanel.completeTreeStructureChange();
		});

		JMenuItem collapseNode = new JMenuItem("Collapse");
		collapseNode.setIcon(IconObtainer.get("collapse.png"));
		collapseNode.setToolTipText("Collapse this node.");
		collapseNode.addActionListener(e -> {
			collapseThisNode(target);
		});

		JMenuItem clearCollapseNode = new JMenuItem("Un-collapse this node");
		clearCollapseNode.setIcon(IconObtainer.get("recoverCollapse.png"));
		clearCollapseNode.setToolTipText("Recover this collapsed node.");
		clearCollapseNode.addActionListener(e -> {
			unCollapseThisNode(target);
		});

		JMenuItem swapNodeJMenuItem = new JMenuItem("Swap nodes");
		swapNodeJMenuItem.setIcon(IconObtainer.get("swapNode.png"));
		swapNodeJMenuItem.setToolTipText("Swap this node with its sibling.");
		swapNodeJMenuItem.addActionListener(e -> {
			swapNode(target);
		});

		clearAnnotatonJmenu4internalNode = new JMenuItem("Clear annotions");
		clearAnnotatonJmenu4internalNode.setIcon(IconObtainer.get("remove.png"));
		clearAnnotatonJmenu4internalNode.setToolTipText("Clear annotations located in this node or descents.");
		clearAnnotatonJmenu4internalNode.addActionListener(e -> {
			Set<GraphicsNode> nodeSet = new HashSet<>();
			TreeOperationUtil.recursiveIterateTreeIF(target, node -> {
				nodeSet.add(node);
			});
			phylogeneticTreePanel.getMainAnnotationsProperties().clearSelectedAnnotation(nodeSet);
			phylogeneticTreePanel.recalculatePreservingZoom();
		});

		JMenuItem displayNodeInformation = new JMenuItem("Display more information");
		displayNodeInformation.setIcon(IconObtainer.get("information.png"));
		displayNodeInformation.setToolTipText("Display more information on node to closer see the details!");
		displayNodeInformation.addActionListener(e -> {
			displayMoreInformation4internalNode(target);
		});

		JMenuItem outputLeavesInforJMenu = new JMenuItem("Export leaf information");
		outputLeavesInforJMenu.setIcon(IconObtainer.get("Export.png"));
		outputLeavesInforJMenu.setToolTipText("Export leaf information as tsv format.");
		outputLeavesInforJMenu.addActionListener(e -> {
			if (!phylogeneticTreePanel.containsTreeNode(target)) return;
			File destination = chooseLeafInformationFile();
			if (destination != null) exportLeavesInformation(destination, target);
		});

		JMenuItem selectAllDescendant = new JMenuItem("Select all descendant nodes");
		selectAllDescendant.setToolTipText("Select this clade and its descendants for subsequent node and branch styling.");
		selectAllDescendant.setIcon(IconObtainer.get("selectall.png"));
		selectAllDescendant.addActionListener(e -> {
			selectedAllDescendants4SingleInternalNode(target);
		});

		JMenuItem clearSelect = new JMenuItem("Set all descendant nodes unselected");
		clearSelect.setToolTipText("Clear selection marks from this clade and its descendants. Preserve colors and annotations.");
		clearSelect.setIcon(IconObtainer.get("clearselect.png"));
		clearSelect.addActionListener(e -> {
			clearSelectionAllDescendants4SingleInternalNode(target);
		});

		popInternalNodeRightClick.add(showInRightByCurrentRoot);
//		JMenuItem showInRightByOriginalRoot = getShowInRightByOriginalRootjMenuItem();
//		popInternalNodeRightClick.add(showInRightByOriginalRoot);

		popInternalNodeRightClick.addSeparator();

		popInternalNodeRightClick.add(getZoomInThisNodeJMenuItem());
		popInternalNodeRightClick.add(selectAllDescendant);
		popInternalNodeRightClick.add(clearSelect);
		popInternalNodeRightClick.addSeparator();

		popInternalNodeRightClick.add(getDisplayNodeNameMenuItem());
		popInternalNodeRightClick.add(displayNodeInformation);
		popInternalNodeRightClick.add(outputLeavesInforJMenu);
		popInternalNodeRightClick.addSeparator();

		popInternalNodeRightClick.add(swapNodeJMenuItem);
		popInternalNodeRightClick.add(ladderizeUp);
		popInternalNodeRightClick.add(ladderizeDown);
		popInternalNodeRightClick.addSeparator();

		popInternalNodeRightClick.add(collapseNode);
		popInternalNodeRightClick.add(clearCollapseNode);
		popInternalNodeRightClick.addSeparator();

		popInternalNodeRightClick.add(annotation);
		popInternalNodeRightClick.add(clearAnnotatonJmenu4internalNode);
		popInternalNodeRightClick.addSeparator();

		JMenuItem branchColor = getRecolorJMenuItem4all();
		popInternalNodeRightClick.add(branchColor);

		popInternalNodeRightClick.addSeparator();
		popInternalNodeRightClick.add(detachNodeJMenuItem());

//		popInternalNodeRightClick.addSeparator();
//		popInternalNodeRightClick.add(getBackMutationPresenterItem());
//		popInternalNodeRightClick.addSeparator();
//		popInternalNodeRightClick.add(getDisplayGenomicMutationPresenterItem());
//		popInternalNodeRightClick.add(getDisplayAAMutationPresenterItem());


		boolean editable = phylogeneticTreePanel.canEditTreeNode(target);
		annotation.setEnabled(editable);
		swapNodeJMenuItem.setEnabled(editable && target != phylogeneticTreePanel.getRootNode() && target.getParentCount() > 0 && target.getParent().getChildCount() > 1);
		ladderizeUp.setEnabled(editable);
		ladderizeDown.setEnabled(editable);
		collapseNode.setEnabled(editable && !target.getCollapse());
		clearCollapseNode.setEnabled(editable && target.getCollapse());
		if (target.getCollapse()) popInternalNodeRightClick.add(collapsePropertiesItem(target));
		clearAnnotatonJmenu4internalNode.setEnabled(phylogeneticTreePanel.getMainAnnotationsProperties().hasAnnotation());
		return prepareMenu(popInternalNodeRightClick);
	}

	protected File chooseLeafInformationFile() {
		EGPSFileChooser chooser = new EGPSFileChooser(this.getClass());
		chooser.setFileFilter(new FileNameExtensionFilter("TSV (*.tsv, *.txt)", "tsv", "txt"));
		return chooser.showSaveDialog() == EGPSFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
	}

	protected void exportLeavesInformation(File localFinalFile) {
		exportLeavesInformation(localFinalFile, currNode);
	}

	protected void exportLeavesInformation(File destination, GraphicsNode target) {
		if (!phylogeneticTreePanel.containsTreeNode(target)) return;
		Path path = destination.toPath();
		String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
		if (!filename.endsWith(".tsv") && !filename.endsWith(".txt")) path = Path.of(path.toString() + ".tsv");
		List<LeafInformation> snapshot = leafInformationSnapshot(target);
		Path output = path;
		CompletableFuture.supplyAsync(() -> Files.exists(output)).whenComplete((exists, checkFailure) ->
				SwingUtilities.invokeLater(() -> {
			if (checkFailure != null) {
				reportExportFailure(output, checkFailure);
				return;
			}
			if (exists && !confirmOverwrite(output)) return;
			CompletableFuture.runAsync(() -> {
				try { writeLeafInformation(output, formatLeafInformation(snapshot)); }
				catch (IOException error) { throw new java.io.UncheckedIOException(error); }
			}).whenComplete((result, failure) -> SwingUtilities.invokeLater(() -> {
				if (failure == null) reportExportResult(null);
				else reportExportFailure(output, failure);
			}));
		}));
	}

	private void reportExportFailure(Path path, Throwable failure) {
		log.error("Unable to export leaf information to {}", path, failure);
		Throwable cause = failure.getCause() == null ? failure : failure.getCause();
		reportExportResult("Unable to export leaf information: " + cause.getMessage());
	}

	protected boolean confirmOverwrite(Path path) {
		return SwingDialog.showConfirmDialog(SwingUtilities.getWindowAncestor(phylogeneticTreePanel),
				"Replace file?", "Replace existing file?\n" + path);
	}

	protected void writeLeafInformation(Path path, String snapshot) throws IOException {
		Files.writeString(path, snapshot, StandardCharsets.UTF_8);
	}

	protected void reportExportResult(String error) {
		if (error == null) SwingDialog.successExportDataDialog();
		else SwingDialog.showErrorMSGDialog("Export leaf information", error);
	}

	public static String leafInformationTsv(GraphicsNode root) {
		return formatLeafInformation(leafInformationSnapshot(root));
	}

	private record LeafInformation(String name, double length, double distance, int rgb, float width, boolean labelEnabled) { }

	private static List<LeafInformation> leafInformationSnapshot(GraphicsNode root) {
		List<LeafInformation> leaves = new ArrayList<>();
		record Entry(GraphicsNode node, double distance) { }
		ArrayDeque<Entry> pending = new ArrayDeque<>();
		pending.push(new Entry(root, 0));
		while (!pending.isEmpty()) {
			Entry entry = pending.pop();
			GraphicsNode node = entry.node();
			if (node.getChildCount() == 0) {
				TreeDrawUnit style = node.getDrawUnit();
				leaves.add(new LeafInformation(node.getName(), node.getRealBranchLength(), entry.distance(),
						style.getLineColor().getRGB(), style.getStrokeSize(), style.isDrawName()));
			} else for (int i = node.getChildCount() - 1; i >= 0; i--) {
				GraphicsNode child = (GraphicsNode) node.getChildAt(i);
				pending.push(new Entry(child, entry.distance() + child.getRealBranchLength()));
			}
		}
		return leaves;
	}

	private static String formatLeafInformation(List<LeafInformation> leaves) {
		StringBuilder text = new StringBuilder("name\treal_branch_length\tdistance_from_clade_root\tbranch_color\tbranch_width\tlabel_enabled\n");
		for (LeafInformation leaf : leaves) {
			text.append(tsvField(leaf.name())).append('\t').append(leaf.length()).append('\t').append(leaf.distance()).append('\t')
					.append(String.format(Locale.ROOT, "#%06X", leaf.rgb() & 0xFFFFFF)).append('\t')
					.append(leaf.width()).append('\t').append(leaf.labelEnabled()).append('\n');
		}
		return text.toString();
	}

	private static String tsvField(String value) {
		if (value == null) return "";
		if (value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('"') >= 0)
			return "\"" + value.replace("\"", "\"\"") + "\"";
		return value;
	}

	/**
	 * 
	 * 右击节点，选择collapse时会调用此方法
	 * 
	 * @title collapseSingleInternalNode
	 * @lastModifiedDate 2022-11-30 13:47
	 * @author
	 * @since 1.7
	 * 
	 * @param node
	 * @return void
	 */
	public void collapseThisNode(GraphicsNode node) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> collapseThisNode(node));
			return;
		}
		if (!phylogeneticTreePanel.canEditTreeNode(node) || node.getChildCount() == 0) return;
		CollapseDialog dialog = createCollapseDialog(node.getDrawUnit().getLineColor());
		dialog.setCallBackConsumer((color, size) -> {
			if (!phylogeneticTreePanel.canEditTreeNode(node)) return;
			CollapseProperty property = new CollapseProperty();
			property.setColor(color.getRed(), color.getGreen(), color.getBlue());
			property.setTriangleSize(size);
			node.setCollapse(true);
			getTreeLayoutProperties().getCollapsePropertyMaps().put(node.getID(), property);
			getTreeLayoutProperties().initializeRoot();
			phylogeneticTreePanel.recalculatePreservingZoom();
		});
		dialog.setVisible(true);
	}

	protected CollapseDialog createCollapseDialog(Color initial) {
		return new CollapseDialog(SwingUtilities.getWindowAncestor(phylogeneticTreePanel), initial);
	}

	private JMenuItem collapsePropertiesItem(GraphicsNode target) {
		JMenuItem item = new JMenuItem("modify collapse property");
		item.setToolTipText("Edit this collapsed clade's color and triangle size.");
		item.setEnabled(phylogeneticTreePanel.canEditTreeNode(target));
		item.addActionListener(e -> {
			if (!phylogeneticTreePanel.canEditTreeNode(target) || !target.getCollapse()) return;
			CollapseProperty property = getTreeLayoutProperties().getCollapsePropertyMaps().get(target.getID());
			CollapseDialog dialog = createCollapseDialog(property == null ? target.getDrawUnit().getLineColor() : property.getColor());
			if (property != null) dialog.setTriangleSize(property.getTriangleSize());
			dialog.setCallBackConsumer((color, size) -> {
				if (!phylogeneticTreePanel.canEditTreeNode(target) || !target.getCollapse()) return;
				CollapseProperty updated = new CollapseProperty();
				updated.setColor(color.getRed(), color.getGreen(), color.getBlue());
				updated.setTriangleSize(size);
				getTreeLayoutProperties().getCollapsePropertyMaps().put(target.getID(), updated);
				phylogeneticTreePanel.recalculatePreservingZoom();
			});
			dialog.setVisible(true);
		});
		return item;
	}

	/**
	 * 
	 * 右击节点，选择uncollapse时会调用此方法
	 * 
	 * @title unCollapseSingleInternalNode
	 * @createdDate 2022-11-30 13:48
	 * @lastModifiedDate 2022-11-30 13:48
	 * @author yjn
	 * @since 1.7
	 * 
	 * @param node
	 * @return void
	 */
	public void unCollapseThisNode(GraphicsNode node) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> unCollapseThisNode(node));
			return;
		}
		if (!phylogeneticTreePanel.canEditTreeNode(node)) return;
		node.setCollapse(false);
		getTreeLayoutProperties().getCollapsePropertyMaps().remove(node.getID());
		getTreeLayoutProperties().initializeRoot();
		phylogeneticTreePanel.recalculatePreservingZoom();
	}

	/**
	 * 
	 * 此方法为右击内节点调用的选中所有子节点
	 * 
	 * @title selectedAllDescendants4InternalNode
	 * @createdDate 2022-11-29 14:13
	 * @lastModifiedDate 2022-11-29 14:13
	 * @author yjn
	 * @since 1.7
	 * 
	 * @param currentNode
	 * @return void
	 */
	protected void selectedAllDescendants4SingleInternalNode(GraphicsNode currentNode) {
		phylogeneticTreePanel.selectSubtree(currentNode, true);
	}

	/**
	 * 此方法为右击内节点调用的取消选中所有子节点
	 * 
	 * @title clearSelcetionAllDescendants4SingleInternalNode
	 * @createdDate 2022-11-30 14:12
	 * @lastModifiedDate 2022-11-30 14:12
	 * @author yjn
	 * @since 1.7
	 * 
	 * @param currentNode
	 * @return void
	 */
	protected void clearSelectionAllDescendants4SingleInternalNode(GraphicsNode currentNode) {
		phylogeneticTreePanel.selectSubtree(currentNode, false);
	}

	/**
	 * display all information for internal node.
	 * 
	 * @title extracted
	 * @createdDate 2021-01-14 21:07
	 * @lastModifiedDate 2021-01-14 21:07
	 * @author yudalang
	 * @param selectedNode
	 * @since 1.7
	 * 
	 * @return void
	 */
	public void displayMoreInformation4internalNode(GraphicsNode selectedNode) {

		List<String> lists = new ArrayList<>();
		lists.add("Name: " + Objects.toString(selectedNode.getName(), ""));
		lists.add("Real branch length: " + selectedNode.getRealBranchLength());
		if (Double.compare(selectedNode.getLength(), selectedNode.getRealBranchLength()) != 0)
			lists.add("Displayed branch length: " + selectedNode.getLength());
		lists.add("Number of child: ".concat(String.valueOf(selectedNode.getChildCount())));
		lists.add("Node size: ".concat(String.valueOf(selectedNode.getSize())));

		DialogMoreInfoGenerator dialog = new DialogMoreInfoGenerator(lists);
		dialog.setTitle("Internal node information");
		dialog.setVisible(true);
	}

	/**
	 * 
	 * 在新的tab中显示当前clade（过滤操作后的状态）的JMenuItem
	 * 
	 * @title getShowInRightByCurrentRootJMenuItem
	 * @createdDate 2020-11-25 15:26
	 * @lastModifiedDate 2020-11-25 15:26
	 * @author yudalang
	 * @since 1.7
	 * 
	 * @return
	 * @return JMenuItem
	 */
	protected JMenuItem getShowInRightByCurrentRootJMenuItem() {
		GraphicsNode target = currNode;
		JMenuItem item = new JMenuItem("View this clade in new tab");
		item.setIcon(IconObtainer.get("tab.png"));
		item.setToolTipText("Open an independent subtree using stored real branch lengths, including collapsed descendants.");
		item.addActionListener(e -> {
			if (!phylogeneticTreePanel.containsTreeNode(target)) return;
			GraphicsNode copy = copySubtreeWithRealLengths(target);
			TreeLayoutProperties properties = LeafLabelStateInitializer.createSubtreeViewProperties(copy, getTreeLayoutProperties());
			IndependentModuleLoader loader = new IndependentModuleLoader();
			loader.setHowModuleLaunchedWithData("Module launched by right click internal node.",
					"Data source is from " + Objects.toString(target.getName(), ""));
			loader.setModuleData(copy, properties);
			openSubtree(loader);
		});
		showInRightByCurrentRoot = item;
		return item;
	}

	protected void openSubtree(IndependentModuleLoader loader) {
		MainFrameProperties.loadTheModuleFromIModuleLoader(loader);
	}

	private JPopupMenu prepareMenu(JPopupMenu menu) {
		Font font = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();
		for (Component component : menu.getComponents()) if (component instanceof JMenuItem item) item.setFont(font);
		return menu;
	}

	public static GraphicsNode copySubtreeWithRealLengths(GraphicsNode source) {
		GraphicsNode copy = EvolNodeUtil.convertNode(source, input -> {
			GraphicsNode original = (GraphicsNode) input;
			GraphicsNode node = new GraphicsNode();
			node.setName(original.getName());
			node.copyBootstrapFrom(original);
			node.setRealBranchLength(original.getRealBranchLength());
			return node;
		});
		copy.setRealBranchLength(0);
		return copy;
	}

	/**
	 * 
	 * 在新的tab中显示当前clade对应的原始树的JMenuItem
	 * 
	 * @title getShowInRightByOriginalRootjMenuItem
	 * @createdDate 2022-08-09 09:55
	 * @lastModifiedDate 2022-08-09 09:55
	 * @author
	 * @since 1.7
	 * 
	 * @return
	 * @return JMenuItem
	 */
	protected JMenuItem getShowInRightByOriginalRootjMenuItem() {
		if (showInRightByOriginalRoot == null) {
			showInRightByOriginalRoot = new JMenuItem("View this clade (incl. hidden strains) in new tab");
			showInRightByOriginalRoot.setIcon(IconObtainer.get("tab.png"));
			showInRightByOriginalRoot.setToolTipText("Showing this clade of original root in a new tab");
			showInRightByOriginalRoot.addActionListener(e -> {
				List<GraphicsNode> selectedNodes = getTreeLayoutProperties().getSelectedNodes();
				if (selectedNodes.size() > 0) {
					GraphicsNode graphicsNode = selectedNodes.get(0);
					int targetID = graphicsNode.getID();

					GraphicsNode originalRootNode = getTreeLayoutProperties().getOriginalRootNode();

					Tuple<GraphicsNode> node2view = new Tuple<GraphicsNode>(null);
					TreeOperationUtil.recursiveIterateLevelTraverse(originalRootNode, node -> {
						if (node.getID() == targetID) {
							node2view.setElement(node);
						}
					});

					if (node2view.getElement() == null) {
						System.err.println("Internal error: " + getClass());
					} else {
//						controller.getMain().addPhylogeneticTreeTab(node2view.getElement());
					}
				}
			});
		}

		return showInRightByOriginalRoot;
	}

	protected void swapNode(GraphicsNode find) {
		if (!phylogeneticTreePanel.canEditTreeNode(find) || find == phylogeneticTreePanel.getRootNode() || find.getParentCount() == 0) return;
		GraphicsNode par = (GraphicsNode) find.getParent();
		int currChildIndex = par.indexOfChild(find);
		int cc = par.getChildCount();
		if (cc > 2) {
			// nodes is the copy of par
			List<GraphicsNode> allChildren = new ArrayList<GraphicsNode>(cc);
			for (int x = 0; x < cc; x++) {
				allChildren.add((GraphicsNode) par.getChildAt(x));
			}
			GraphicsNode child1 = (GraphicsNode) par.getChildAt(currChildIndex);
			GraphicsNode child2 = null;
			if ((currChildIndex + 1) < cc) {
				child2 = (GraphicsNode) par.getChildAt(currChildIndex + 1);
				par.removeAllChild();
				for (int n = 0; n < cc; n++) {
					if (n == currChildIndex) {
						par.addChild(child2);
						par.addChild(child1);
						n = n + 1;
					} else {
						par.addChild(allChildren.get(n));
					}
				}
			} else {
				// this means swap the last node and the first node
				child2 = (GraphicsNode) par.getChildAt(0);
				par.removeAllChild();
				par.addChild(child1);
				for (int n = 1; n < cc - 1; n++) {
					par.addChild(allChildren.get(n));
				}
				par.addChild(child2);
			}
		} else if (cc == 2) {
			GraphicsNode child1 = (GraphicsNode) par.getChildAt(0);
			GraphicsNode child2 = (GraphicsNode) par.getChildAt(1);
			par.removeAllChild();
			par.addChild(child2);
			par.addChild(child1);
		} else {
			// 时间filter后，会有节点下只有一个child的情况，所以增加一个保护
		}

		phylogeneticTreePanel.completeTreeStructureChange();
	}

//	public JPopupMenu getLeafInfoPop(GraphicsNode node) {
//
//		JPopupMenu leafNodeInfo = new JPopupMenu();
//		JMenuItem leafNodeInfo_nodeName = new JMenuItem();
//		JMenuItem leafNodeInfo_branchLength = new JMenuItem();
//		JMenuItem leaftNodeInfo_mutation = new JMenuItem();
//		JMenuItem leaftNodeInfo_date = new JMenuItem();
//		JMenuItem leaftNodeInfo_age = new JMenuItem();
//		JMenuItem leaftNodeInfo_sexul = new JMenuItem();
//		JMenuItem leaftNodeInfo_country = new JMenuItem();
//		JMenuItem leaftNodeInfo_province = new JMenuItem();
//		leafNodeInfo.add(leafNodeInfo_nodeName);
//		leafNodeInfo.add(leafNodeInfo_branchLength);
//		leafNodeInfo.add(leaftNodeInfo_mutation);
//		leafNodeInfo.add(leaftNodeInfo_date);
//		leafNodeInfo.add(leaftNodeInfo_age);
//		leafNodeInfo.add(leaftNodeInfo_sexul);
//		leafNodeInfo.add(leaftNodeInfo_country);
//		leafNodeInfo.add(leaftNodeInfo_province);
//		leafNodeInfo.setFocusable(false);
//
//		String[] leafTooltipInformation = getLeafTooltipInformation(node);
//		leafNodeInfo_nodeName.setText(leafTooltipInformation[0]);
//		leafNodeInfo_branchLength.setText(leafTooltipInformation[1]);
//		leaftNodeInfo_mutation.setText(leafTooltipInformation[2]);
//		leaftNodeInfo_date.setText(leafTooltipInformation[3]);
//		leaftNodeInfo_age.setText(leafTooltipInformation[4]);
//		leaftNodeInfo_sexul.setText(leafTooltipInformation[5]);
//		leaftNodeInfo_country.setText(leafTooltipInformation[6]);
//		leaftNodeInfo_province.setText(leafTooltipInformation[7]);
//
//		return leafNodeInfo;
//	}

	/**
	 * 注意顺序！
	 *
	 * @param node
	 * @return
	 * @throws ParseException
	 */
	public List<String> getLeafTooltipInformation(GraphicsNode node, boolean showAllOrigin) {
		List<String> informations = new ArrayList<>();

		informations.add("Leaf name: ".concat(String.valueOf(node.getName())));
		informations.add("CGB ID: ".concat(String.valueOf(node.getCGBID())));
		informations.add("Branch length:  ".concat(String.valueOf(getBranchLength(node))));

		informations.add("");

		informations.add("");

//		DateTimeFormatter humanReadableDataFormat = controller.getHumanReadableDataFormat();

		informations.add(BUTTOM_HTML_STRING);

		if (debug) {
			informations.add("");
			informations.add("Debug information:");
			informations.add("Node ID: " + node.getID());
		}
		return informations;
	}

	protected int getNumOfChildren(GraphicsNode node) {
		return node.getChildCount();
	}

	protected String getBranchLength(GraphicsNode node) {

		double displayedBranchLength = node.getDisplayedBranchLength();
		String ret = deciFor.format(displayedBranchLength);

		BaseLayout currLayout = phylogeneticTreePanel.getCurrLayout();
		BranchLengthType branchLengthType = currLayout.getBranchLengthType();

		switch (branchLengthType) {
		case DIVERGENCE:

			break;
		case MUTATION_COUNT:
			if (displayedBranchLength > 1) {
				ret += " counts";
			} else {
				ret += " count";
			}
			break;
		default:
			break;
		}

		return ret;
	}

	private JMenuItem getClearAnnotaton() {
		if (clearAnnotaton4blinkArea == null) {
			clearAnnotaton4blinkArea = new JMenuItem("Clear all annotions");
			clearAnnotaton4blinkArea.setIcon(IconObtainer.get("remove.png"));
			clearAnnotaton4blinkArea.setToolTipText("Clear all annotations.");
			clearAnnotaton4blinkArea.addActionListener(e -> {
				phylogeneticTreePanel.getMainAnnotationsProperties().clearAllAnnotation();
				phylogeneticTreePanel.updatePhylogeneticTreePanel4fitFrame();
			});
		}
		return clearAnnotaton4blinkArea;
	}

	private JMenuItem getClearCollapseNode() {
		if (clearCollapseNode == null) {
			clearCollapseNode = new JMenuItem("Un-collapse all nodes");
			clearCollapseNode.setIcon(IconObtainer.get("recoverCollapse.png"));
			clearCollapseNode.setToolTipText("Recover all collapsed nodes.");
			clearCollapseNode.addActionListener(e -> {
				clearAllCollapsedNodes();
			});
		}
		return clearCollapseNode;
	}

	/**
	 * 
	 * 右击空白处可以选择清空所有collapse的节点
	 * 
	 * @title clearAllCollapsedNodes
	 * @createdDate
	 * @lastModifiedDate 2022-11-30 15:14
	 * @author
	 * @since 1.7
	 * 
	 * @return void
	 */
	protected void clearAllCollapsedNodes() {
		if (!phylogeneticTreePanel.isTreeLayoutReady()) return;
		for (GraphicsNode node : PhylogeneticTreePanel.subtreeNodes(getTreeLayoutProperties().getOriginalRootNode())) node.setCollapse(false);
		for (GraphicsNode node : PhylogeneticTreePanel.subtreeNodes(phylogeneticTreePanel.getRootNode())) node.setCollapse(false);
		getTreeLayoutProperties().clearCollapseProproties();
		phylogeneticTreePanel.recalculatePreservingZoom();
	}

	private JMenuItem getRefreshPhylogeneticTreePanel() {
		if (refreshPhylogeneticTreePanel == null) {
			refreshPhylogeneticTreePanel = new JMenuItem("Refresh ( Fit frame )");
			refreshPhylogeneticTreePanel.setIcon(IconObtainer.get("refresh.png"));
			refreshPhylogeneticTreePanel.setToolTipText("Refresh phylogenetic tree panel.");
			refreshPhylogeneticTreePanel.addActionListener(e -> {
				phylogeneticTreePanel.updatePhylogeneticTreePanel4fitFrame();
			});
		}
		return refreshPhylogeneticTreePanel;
	}

	private JMenuItem getRecover2defaultGraphicEffects() {
		if (recover2defaultGraphicEffects == null) {
			recover2defaultGraphicEffects = new JMenuItem("Recover all graphics effects");
			recover2defaultGraphicEffects.setIcon(IconObtainer.get("default.png"));
			recover2defaultGraphicEffects.setToolTipText("Restore branch colors, line widths, node sizes and fonts. Keep labels, annotations and collapse settings.");
			recover2defaultGraphicEffects.addActionListener(e -> {
				recoverAllGraphicsEffects();
			});
		}
		return recover2defaultGraphicEffects;
	}

	protected void recoverAllGraphicsEffects() {
		TreeOperationUtil.recursiveIterateTreeIF(getTreeLayoutProperties().getOriginalRootNode(), node -> {
			node.getDrawUnit().recover2PrimaryStates();
		});

		getTreeLayoutProperties().recover2defaultFont();
		phylogeneticTreePanel.updatePhylogeneticTreePanel4fitFrame();
	}

	private JMenuItem getRemoveAllSelectionsJMenuItem() {
		if (removeAllSelections == null) {
			removeAllSelections = new JMenuItem("Cancel all selections");
			removeAllSelections.setIcon(IconObtainer.get("no_select.png"));
			removeAllSelections.setToolTipText("Remove all selections( Added by search and left click.)");
			removeAllSelections.addActionListener(e -> {
				removeAllSelections();
			});
		}
		return removeAllSelections;
	}

	/**
	 * 
	 * 清除树上的所有选中
	 * 
	 * @title removeAllSelections
	 * @createdDate
	 * @lastModifiedDate 2022-11-30 15:19
	 * @author
	 * @since 1.7
	 * 
	 * @return void
	 */
	protected void removeAllSelections() {
		phylogeneticTreePanel.clearNodeSelection();
	}

	private JMenuItem getRemoveCustomizedColors() {
		if (removeCustomizedColors == null) {
			removeCustomizedColors = new JMenuItem("Remove customized colors");
			removeCustomizedColors.setIcon(IconObtainer.get("cancel_recolor.png"));
			removeCustomizedColors
					.setToolTipText("Remove customized colors( Added by right click or allele freq. bar.)");
			removeCustomizedColors.addActionListener(e -> {
				SwingUtilities.invokeLater(() -> {
//					int beforeRenderingIndex = controller.getCustomizedSaveBean().getBeforeRenderingIndex();
					// controller.changeColorScheme(ColorScheme.getColorSchemeAccording2index(beforeRenderingIndex));
					// color scheme的面板的选择状态也要修改，所以就直接修改选择状态好了。选择状态修改了，会自动调用changeColorScheme()的
//					controller.getLeftControlPanelContainner().setCustomizedButtonSelected(beforeRenderingIndex);
				});
			});
		}
		return removeCustomizedColors;
	}

	/**
	 * 
	 * 此方法为右击空白处调用
	 * 
	 * @title getSelectDescendants
	 * @createdDate 2022-11-29 14:11
	 * @lastModifiedDate 2022-11-29 14:11
	 * @author
	 * @since 1.7
	 * 
	 * @return
	 * @return JMenuItem
	 */
	protected JMenuItem getSelectDescendantsJMenuItem() {
		if (selectDescendants == null) {
			selectDescendants = new JMenuItem("Select descendants of marked nodes");
			selectDescendants.setIcon(IconObtainer.get("selectall.png"));
			selectDescendants.setToolTipText("Select all descendants of selected nodes.");
			selectDescendants.addActionListener(e -> {
				selectAllDescendants();
			});
		}
		return selectDescendants;
	}

	/**
	 * 
	 * 选中所有当前选中节点的子节点
	 * 
	 * @title selectAllDescendants
	 * @createdDate
	 * @lastModifiedDate 2022-11-30 19:47
	 * @author
	 * @since 1.7
	 * 
	 * @return void
	 */
	protected void selectAllDescendants() {
		Set<GraphicsNode> seeds = new LinkedHashSet<>(getTreeLayoutProperties().getSelectedNodes());
		for (GraphicsNode node : PhylogeneticTreePanel.subtreeNodes(phylogeneticTreePanel.getRootNode())) {
			if (node.getDrawUnit().isNodeSelected()) seeds.add(node);
		}
		for (GraphicsNode seed : seeds) phylogeneticTreePanel.selectSubtree(seed, true);
	}


	protected void exportPicturesOrData() {

//		controller.saveViewPanelAs();
	}


	public void refreshMutationFrequency() {
		Runnable act = () -> {
//			controller.letGenomeViewUpdateStaticBarPlot(treeLayoutProperties.getOriginalRootNode());
		};
	}

	public JPopupMenu getLeafRightClickPop(GraphicsNode currNodes, Component pan) {
		if (currNodes.getChildCount() > 0) return getInternalNodeRightClickPop(currNodes);
		currNode = currNodes;
		assembleLeafRightClickPopMenu();
		return prepareMenu(popLeafRightClick);
	}

	protected void assembleLeafRightClickPopMenu() {
		GraphicsNode target = currNode;
		popLeafRightClick = new JPopupMenu();
		popLeafRightClick.add(getDisplayNodeNameMenuItem());
		JMenuItem info = new JMenuItem("Display more information");
		info.setIcon(IconObtainer.get("information.png"));
		info.setToolTipText("Show this node's name and real branch length, plus displayed length when different.");
		info.addActionListener(e -> displayMoreInformation4leaf(target));
		popLeafRightClick.add(info);
		popLeafRightClick.add(getZoomInThisNodeJMenuItem());
		popLeafRightClick.add(getRecolorJMenuItem4all());
		popLeafRightClick.addSeparator();
		JMenuItem swap = new JMenuItem("Swap nodes");
		swap.setIcon(IconObtainer.get("swapNode.png"));
		swap.setToolTipText("Swap this node with its sibling without changing branch lengths.");
		swap.setEnabled(phylogeneticTreePanel.canEditTreeNode(target) && target.getParentCount() > 0 && target.getParent().getChildCount() > 1);
		swap.addActionListener(e -> {
			swapNode(target);
		});
		popLeafRightClick.add(swap);
		popLeafRightClick.add(detachNodeJMenuItem());
	}

	public JPopupMenu getInternalNodeRightClickPop(GraphicsNode currNodes) {
		currNode = currNodes;
		return getPopInternalNodeRightClick();
	}

	public JPopupMenu getBlinkAreaPop(Component component, Point point) {
		Point anchor = new Point(point);
		blinkAreaPopupMenu = new JPopupMenu();
		// These tree-wide actions have no node context; each menu still owns its components.
		refreshPhylogeneticTreePanel = recover2defaultGraphicEffects = clearAnnotaton4blinkArea = clearCollapseNode = null;
		removeAllSelections = selectDescendants = null;
		blinkAreaPopupMenu.add(getRefreshPhylogeneticTreePanel());
		blinkAreaPopupMenu.add(getRecover2defaultGraphicEffects());
		blinkAreaPopupMenu.addSeparator();
		JMenuItem annotations = getClearAnnotaton();
		annotations.setEnabled(phylogeneticTreePanel.getMainAnnotationsProperties().hasAnnotation());
		blinkAreaPopupMenu.add(annotations);
		JMenuItem expand = getClearCollapseNode();
		expand.setEnabled(hasCollapsedNode() && phylogeneticTreePanel.isTreeLayoutReady());
		blinkAreaPopupMenu.add(expand);
		blinkAreaPopupMenu.add(getRemoveAllSelectionsJMenuItem());
		blinkAreaPopupMenu.add(getSelectDescendantsJMenuItem());
		blinkAreaPopupMenu.addSeparator();
		JMenuItem zoom = new JMenuItem("Zoom the area");
		zoom.setIcon(IconObtainer.get("zoomIn2.png"));
		zoom.setToolTipText("Zoom around this right-click position.");
		zoom.setEnabled(phylogeneticTreePanel.isTreeLayoutReady());
		zoom.addActionListener(e -> {
			if (phylogeneticTreePanel.isTreeLayoutReady()) phylogeneticTreePanel.continuouslyZoomInOrOut(1000, 1000, anchor);
		});
		blinkAreaPopupMenu.add(zoom);
		return prepareMenu(blinkAreaPopupMenu);
	}

	/**
	 * 
	 * 判定当前树上是否有collapse的节点
	 * 
	 * @title hasCollapsedNode
	 * @createdDate
	 * @lastModifiedDate 2022-11-30 14:58
	 * @author
	 * @since 1.7
	 * 
	 * @return
	 * @return boolean
	 */
	protected boolean hasCollapsedNode() {
		return !getTreeLayoutProperties().getCollapsePropertyMaps().isEmpty();
	}

	/**
	 * 指定日期后几天
	 *
	 * @param date  指定日期
	 * @param count 天数
	 * @return
	 */
	protected String getAfterDay(LocalDate date, int count, DateTimeFormatter df) {
		LocalDate minusDays = date.plusDays(count);
		return df.format(minusDays);
	}

	/**
	 * 指定日期前几天
	 *
	 * @param date  指定日期
	 * @param count 天数
	 * @return
	 */
	protected String getBeforeDay(LocalDate date, int count, DateTimeFormatter df) {
		LocalDate minusDays = date.minusDays(count);
		return df.format(minusDays);
	}

}
