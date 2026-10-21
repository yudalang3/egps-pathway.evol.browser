package module.evolview.gfamily.work.listener;

import egps2.UnifiedAccessPoint;
import egps2.panels.dialog.SwingDialog;
import evoltree.struct.util.EvolNodeUtil;
import evoltree.txtdisplay.TreeDrawUnit;
import module.evolview.gfamily.work.gui.ScaleBarRectObject;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.gfamily.work.gui.tree.RectObj;
import module.evolview.gfamily.work.gui.tree.TreePopupMenu;
import module.evolview.common.SwingDebouncer;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.apache.commons.compress.utils.Lists;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TreeListener extends MouseAdapter implements KeyListener {

	private static final long TIME_DIFFERENCE_FOR_IDLE = 300;
	private long lastMoveTime;

	private final TreeLayoutProperties treeLayoutProperties;
	private final List<Consumer<GraphicsNode>> tree2AnalyzingPanelInteractions = Lists.newArrayList();

	private GraphicsNode rootNode;
	private final PhylogeneticTreePanel phylogeneticTreePanel;

	private TreePopupMenu jPopupMenu4normalMode;

	private final Line2D.Double repeatLine = new Line2D.Double();

	@SuppressWarnings("deprecation") // Lazy storage for the public compatibility accessor only.
	private RectObj scaleBarRectAdjust;

	private Point origin;
	private boolean isDragging = false;
	private enum Gesture { NONE, SELECT, PAN, EDIT }
	private Gesture gesture = Gesture.NONE;
	private MouseEvent pressedEvent;
	private GroupDragSession groupDrag;

	private final StringBuilder sBuilder = new StringBuilder(1024);
	private final String templateHeader = "<html><body font size='5'>";
	private final String templatefooter = "</body></html>";
	private final SwingDebouncer wheelZoomDebouncer = new SwingDebouncer(40, this::applyPendingWheelZoom);
	private int pendingWheelHeightChange;
	private int pendingWheelWidthChange;
	private Point pendingWheelPoint;

	private final Rectangle rect4selectArea = new Rectangle();
	private final String gestureActionHint = new String(
			"<html><body font size='5'> Ctrl + left click to toggle selection.<br> Ctrl + drag to add nodes.<br> Shift + left click to collapse.<br> Alt + left click to see info.<br> Right click to operate.</body></html>");

	public TreeListener(TreeLayoutProperties properties, GraphicsNode rootNode,
			PhylogeneticTreePanel phylogeneticTreePanel) {
		Objects.requireNonNull(properties);
		Objects.requireNonNull(rootNode);
		Objects.requireNonNull(phylogeneticTreePanel);
		this.treeLayoutProperties = properties;
		this.rootNode = rootNode;
		this.phylogeneticTreePanel = phylogeneticTreePanel;

	}

	public void setRootNode(GraphicsNode rootNode) {
		resetGesture();
		this.rootNode = rootNode;
	}

	public TreeLayoutProperties getTreeLayoutProperties() {
		return treeLayoutProperties;
	}

	/**
	 * @deprecated Retained for binary compatibility. Scale bars no longer handle mouse gestures.
	 */
	@Deprecated
	public RectObj getScaleBarListenerObj() {
		if (scaleBarRectAdjust == null) {
			scaleBarRectAdjust = new ScaleBarRectObject(phylogeneticTreePanel.getScaleBarProperty());
		}

		return scaleBarRectAdjust;
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (origin == null || gesture == Gesture.NONE) return;
		if (!isDragging && origin.distanceSq(arg0.getPoint()) < 16) return;
		if (!isDragging && gesture == Gesture.EDIT && treeLayoutProperties.isCreativeMode()
				&& !phylogeneticTreePanel.isLayoutCalculationPending()
				&& phylogeneticTreePanel.getCurrLayout() != null) {
			double ratio = phylogeneticTreePanel.getCurrLayout().getCanvas2logicRatio();
			if (Double.isFinite(ratio) && ratio > 0) {
				groupDrag = new GroupDragSession(treeLayoutProperties.getSelectedNodes(), rootNode, ratio);
			}
		}
		isDragging = true;
		switch (gesture) {
			case SELECT -> {
				Point end = arg0.getPoint();
				rect4selectArea.setFrame(Math.min(origin.x, end.x), Math.min(origin.y, end.y),
						Math.abs(end.x - origin.x), Math.abs(end.y - origin.y));
				phylogeneticTreePanel.repaint();
			}
			case EDIT -> {
				if (groupDrag != null && treeLayoutProperties.isCreativeMode()
						&& !phylogeneticTreePanel.isLayoutCalculationPending()) {
					groupDrag.move(arg0.getX() - origin.x);
					phylogeneticTreePanel.repaint();
				}
			}
			case PAN -> {
				arg0.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				moveTheDrawingArea(arg0);
			}
			default -> { }
		}
	}

	/** Snapshot once, then apply a single bounded displacement to the whole selection. */
	private static final class GroupDragSession {
		private final Map<GraphicsNode, Double> positions = new LinkedHashMap<>();
		private final Map<GraphicsNode, Double> lengths = new LinkedHashMap<>();
		private final double ratio;
		private double minimum = Double.NEGATIVE_INFINITY;
		private double maximum = Double.POSITIVE_INFINITY;

		GroupDragSession(List<GraphicsNode> selected, GraphicsNode root, double ratio) {
			this.ratio = ratio;
			for (GraphicsNode node : selected) {
				if (node != root && node.getParentCount() > 0 && !node.hideNode()) {
					positions.put(node, node.getXSelf());
				}
			}
			for (GraphicsNode node : positions.keySet()) {
				lengths.put(node, node.getRealBranchLength());
				for (int i = 0; i < node.getChildCount(); i++) {
					GraphicsNode child = (GraphicsNode) node.getChildAt(i);
					lengths.put(child, child.getRealBranchLength());
				}
			}
			for (var edge : lengths.entrySet()) {
				int direction = direction(edge.getKey());
				if (direction > 0) minimum = Math.max(minimum, -edge.getValue() * ratio);
				if (direction < 0) maximum = Math.min(maximum, edge.getValue() * ratio);
			}
		}

		private int direction(GraphicsNode child) {
			return (positions.containsKey(child) ? 1 : 0)
					- (positions.containsKey((GraphicsNode) child.getParent()) ? 1 : 0);
		}

		void move(double requested) {
			double delta = Math.max(minimum, Math.min(maximum, requested));
			positions.forEach((node, x) -> node.setXSelf(x + delta));
			lengths.forEach((child, length) -> {
				child.setXParent(((GraphicsNode) child.getParent()).getXSelf());
				int direction = direction(child);
				if (direction != 0) child.setRealBranchLength(Math.max(0, length + direction * delta / ratio));
			});
		}
	}

	private void moveTheDrawingArea(MouseEvent arg0) {
		JViewport viewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, phylogeneticTreePanel);
		if (viewPort == null) {
			SwingDialog.showErrorMSGDialog("Internal error", "The tree panel should be add in the JScrollPanel.");
			return;
		}
		Rectangle viewArea = viewPort.getViewRect();

		int deltaX = origin.x - arg0.getX();
		int deltaY = origin.y - arg0.getY();


		viewArea.x += deltaX;
		viewArea.y += deltaY;

		phylogeneticTreePanel.scrollRectToVisible(viewArea);

		if (UnifiedAccessPoint.isGULaunched()) UnifiedAccessPoint.getInstanceFrame()
				.showTipsOnBottomStatusBar("Press Ctrl key with drag to select nodes.");

	}

	@Override
	public void mouseMoved(MouseEvent e) {

		long currentTime = System.currentTimeMillis();
		long diff = currentTime - lastMoveTime;

		// 每两次tooltip的请求发送需要有一定的间隔
		if (diff <= TIME_DIFFERENCE_FOR_IDLE) {
			return;
		}

		lastMoveTime = System.currentTimeMillis();

		Point point = e.getPoint();

		Component component = e.getComponent();

		Optional<GraphicsNode> optRet = judgeSelectedNode(point);
		// Need to complete
		if (optRet.isPresent()) {
			phylogeneticTreePanel.setToolTipText(treeLayoutProperties.isCreativeMode()
					? "Creative mode is on. Ctrl-click to toggle selection; Ctrl-drag to add nodes. Drag selected nodes horizontally together; the root stays fixed. Right-click to edit nodes."
					: gestureActionHint);
		} else {
			component.setCursor(Cursor.getDefaultCursor());
			phylogeneticTreePanel.setToolTipText(null);
			return;
		}
//		GraphicsNode selectedNode = optRet.get();
//		boolean isLeaf = selectedNode.getChildCount() == 0;
//
//		String tooltipString = null;
//		// 在进行filter后内节点可能变成叶子节点，所以不能单通过判断是否有child判断叶子，加上了name是否为空的判断
//		if (isLeaf && !selectedNode.getName().isEmpty()) {
//			tooltipString = "Leaf node: ".concat(selectedNode.getName());
//		} else {
//			tooltipString = "fefefewafew";
//		}
//
//		phylogeneticTreePanel.setToolTipText(tooltipString);

		// 鼠标挪动的位置有节点则显示手势
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

	}

	public String getTooltipString(List<String> contents) {
		sBuilder.setLength(0);
		sBuilder.append(templateHeader);

		for (String string : contents) {
			sBuilder.append("  ");
			sBuilder.append(string);
			sBuilder.append("  ");
			sBuilder.append("<br>");
		}

//		sBuilder.append("<style=\"font-family:verdana;font-size:4;color:white\">" + 
//				"<i>Click to see more information<br>" + 
//				"Right click and open new tab to see clade</i>" +
//				"</style>");

		sBuilder.append(templatefooter);

		return sBuilder.toString();
	}

	public Optional<GraphicsNode> judgeSelectedNode(Point point) {

		Predicate<GraphicsNode> func = node -> {
			return ifPointLocatedInNode(point, node);
		};
		Optional<GraphicsNode> searchNodeWithReturn = EvolNodeUtil.searchNodeWithReturn(rootNode, func);
		return searchNodeWithReturn;
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		arg0.getComponent().setCursor(Cursor.getDefaultCursor());
		if (pressedEvent == null || !SwingUtilities.isLeftMouseButton(arg0)) return;
		if (isDragging) {
			if (gesture != Gesture.PAN) mouseDragged(arg0);
			if (gesture == Gesture.SELECT) {
				Set<GraphicsNode> selection = new LinkedHashSet<>(treeLayoutProperties.getSelectedNodes());
				EvolNodeUtil.recursiveIterateTreeIFWithStop(rootNode, (GraphicsNode node) -> {
					if (!node.hideNode() && rect4selectArea.contains(node.getXSelf(), node.getYSelf())) {
						selection.add(node);
						node.getDrawUnit().setBranchSelected(true);
					}
					return node.getCollapse();
				});
				List<GraphicsNode> selected = treeLayoutProperties.getSelectedNodes();
				selected.clear();
				selected.addAll(selection);
			}
			phylogeneticTreePanel.notifyTreeViewStateChanged();
			refreshPropertiesPanel();
		} else if (gesture != Gesture.NONE) {
			actionForMouseClick(pressedEvent);
		}
		resetGesture();
		phylogeneticTreePanel.weakRefreshViewPort();
	}

	private void resetGesture() {
		rect4selectArea.setSize(0, 0);
		origin = null;
		pressedEvent = null;
		groupDrag = null;
		gesture = Gesture.NONE;
		isDragging = false;
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		resetGesture();
		origin = new Point(arg0.getPoint());
		if (!SwingUtilities.isLeftMouseButton(arg0) || (!arg0.isControlDown()
				&& (arg0.isAltDown() || arg0.isShiftDown()))) {
			actionForMouseClick(arg0);
			return;
		}
		pressedEvent = arg0;
		if (arg0.isControlDown()) {
			gesture = Gesture.SELECT;
			return;
		}
		if (treeLayoutProperties.isCreativeMode()) {
			Optional<GraphicsNode> hit = judgeSelectedNode(arg0.getPoint());
			if (hit.isPresent() && !treeLayoutProperties.getSelectedNodes().contains(hit.get())) {
				dealWithSelectedNodes(hit.get());
			}
			gesture = treeLayoutProperties.getSelectedNodes().isEmpty() ? Gesture.PAN : Gesture.EDIT;
		} else {
			gesture = Gesture.PAN;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// 这是一个高级实践，因为我们已经有其它的实现了是，所以这个方法设置为空
	}

	protected void actionForMouseClick(MouseEvent e) {
		Optional<GraphicsNode> optRet = judgeSelectedNode(e.getPoint());

		Component component = e.getComponent();

		if (SwingUtilities.isRightMouseButton(e)) {// 鼠标右击操作
			mouseRightClickAction(e, optRet, component);
		} else {
			// 鼠标左键
			boolean controlDown = e.isControlDown();
			boolean altDown = e.isAltDown();
			boolean shiftDown = e.isShiftDown();

			if (controlDown) {
				if (optRet.isPresent()) {
					GraphicsNode selectedNode = optRet.get();
					controlPressSelect(selectedNode);
				}
			} else if (altDown) {
				if (optRet.isPresent()) {
					GraphicsNode selectedNode = optRet.get();
					dealWithSelectedNodes(selectedNode);

//					boolean isLeaf = TreeDecideUtil.decideNodeTypeOfLeaf(selectedNode);
					// yudalang: 这里是显示具体的信息，所以判定就直接用 getChildCount==0来好了。
					boolean isLeaf = selectedNode.getChildCount() == 0;
					if (isLeaf) {
						getTreePopupMenu().displayMoreInformation4leaf(selectedNode);
					} else {
						getTreePopupMenu().displayMoreInformation4internalNode(selectedNode);
					}
				} else {
					dealWithSelectedNodes(null);
				}

			} else if (shiftDown) {

				if (optRet.isPresent()) {
					GraphicsNode selectedNode = optRet.get();
					dealWithSelectedNodes(selectedNode);

					// Collapsed clades remain structural internal nodes and can be expanded again.
					if (selectedNode.getChildCount() > 0) {
						if (selectedNode.getCollapse()) getTreePopupMenu().unCollapseThisNode(selectedNode);
						else getTreePopupMenu().collapseThisNode(selectedNode);
					}
				} else {
					dealWithSelectedNodes(null);
				}

			} else {

				// 单纯的左击
				if (optRet.isPresent()) {
					GraphicsNode selectedNode = optRet.get();
					dealWithSelectedNodes(selectedNode);

					for (Consumer<GraphicsNode> consumer : tree2AnalyzingPanelInteractions) {
						consumer.accept(selectedNode);
					}
				} else {
					dealWithSelectedNodes(null);
				}
				// Ordinary clicks are dispatched on release so a drag retains the selection.

			}
		}

		refreshPropertiesPanel();
		phylogeneticTreePanel.refreshViewPort();

	}

	private void refreshPropertiesPanel() {
		if (UnifiedAccessPoint.isGULaunched()) {
			UnifiedAccessPoint.getInstanceFrame().refreshRightGraphicPropertiesPanel();
		}
	}

	private void mouseRightClickAction(MouseEvent e, Optional<GraphicsNode> optRet, Component component) {
		if (optRet.isPresent()) {
			// 当有节点被选中时
			GraphicsNode selectedNode = optRet.get();
			TreeLayoutProperties treeLayoutProperties2 = getTreeLayoutProperties();
			List<GraphicsNode> selectedNodes = treeLayoutProperties2.getSelectedNodes();
			if (selectedNodes.contains(selectedNode)) {

			} else {
				dealWithSelectedNodes(selectedNode);
			}

			if (treeLayoutProperties2.isCreativeMode()) {
				JPopupMenu popup = getPopupMenu4creativeMode(selectedNode);
				popup.show(component, e.getX(), e.getY());
			} else {
				boolean isLeaf = selectedNode.getChildCount() == 0;
				// Collapse changes presentation, not the node's structural role.
				if (isLeaf) {
					JPopupMenu popup = getTreePopupMenu().getLeafRightClickPop(selectedNode, component);
					popup.show(component, e.getX(), e.getY());
				} else {
					JPopupMenu popup = getTreePopupMenu().getInternalNodeRightClickPop(selectedNode);
					popup.show(component, e.getX(), e.getY());
				}
			}

		} else {
			// 当没有节点的时候
			JPopupMenu popup = getTreePopupMenu().getBlinkAreaPop(component, e.getPoint());
			popup.show(component, e.getX(), e.getY());
		}
	}

	/**
	 * 
	 * 按住control的多节点选中
	 * 
	 * @title controlPressSelect
	 * @createdDate 2022-11-29 15:27
	 * @lastModifiedDate 2022-11-29 15:27
	 * @author
	 * @since 1.7
	 * 
	 * @param selectedNode
	 * @return void
	 */
	protected void controlPressSelect(GraphicsNode selectedNode) {

		List<GraphicsNode> selectedNodes = getTreeLayoutProperties().getSelectedNodes();
		boolean removed = selectedNodes.removeIf(node -> node.equals(selectedNode));
		if (!removed) selectedNodes.add(selectedNode);
		selectedNode.getDrawUnit().setBranchSelected(!removed);
		phylogeneticTreePanel.notifyTreeViewStateChanged();
		phylogeneticTreePanel.refreshViewPort();
	}

	protected void dealWithSelectedNodes(GraphicsNode selectedNode) {

		List<GraphicsNode> selectedNodes = getTreeLayoutProperties().getSelectedNodes();
		for (GraphicsNode node : selectedNodes) {
			node.getDrawUnit().setBranchSelected(false);
		}
		selectedNodes.clear();

		if (selectedNode == null) {
			phylogeneticTreePanel.notifyTreeViewStateChanged();
			if (UnifiedAccessPoint.isGULaunched()) UnifiedAccessPoint.getInstanceFrame().showTipsOnBottomStatusBar(null);
			return;
		}

		TreeDrawUnit bioLine = selectedNode.getDrawUnit();
		bioLine.setBranchSelected(true);
		selectedNodes.add(selectedNode);
		phylogeneticTreePanel.notifyTreeViewStateChanged();

		sBuilder.setLength(0);
		sBuilder.append("The node \"");
		sBuilder.append(selectedNode.getName());
		sBuilder.append("\" selected, ");
		sBuilder.append("child count is: ");
		sBuilder.append(selectedNode.getChildCount());
		sBuilder.append(", branch length is: ");
		sBuilder.append(selectedNode.getLength());

		if (UnifiedAccessPoint.isGULaunched()) UnifiedAccessPoint.getInstanceFrame().showTipsOnBottomStatusBar(sBuilder.toString());
	}

	private boolean ifPointLocatedInNode(Point point, GraphicsNode node) {
		// 如果是隐藏的属性的话，那么应该直接返回false
		if (node.hideNode()) {
			return false;
		}
		for (GraphicsNode ancestor = node; ancestor != rootNode && ancestor.getParentCount() > 0;) {
			ancestor = (GraphicsNode) ancestor.getParent();
			if (ancestor.getCollapse()) return false;
		}

		double xSelf = node.getXSelf();
		double ySelf = node.getYSelf();

		double distanceSq = point.distanceSq(xSelf, ySelf);

		if (distanceSq < 10 * 10) {
			return true;
		}

		double xParent = node.getXParent();
		double yParent = node.getYParent();

		repeatLine.setLine(xSelf, ySelf, xParent, yParent);
		double distance = repeatLine.ptSegDist(point);

//		System.out.println("------------------------------------------------------");
//		System.out.println(xParent + "\t" + yParent + "\t" + xSelf + "\t" + ySelf);
//		System.out.println(point + "\t" + node.getID() + "\t" + node.getName() + "\t" + distance);
		if (distance <= 1.5) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int scrollAmount = -1 * 10 * e.getUnitsToScroll();

		boolean whetherHeightScaleOnMouseWheel = treeLayoutProperties.isWhetherHeightScaleOnMouseWheel();
		boolean whetherWidthScaleOnMouseWheel = treeLayoutProperties.isWhetherWidthScaleOnMouseWheel();

		int heightScrollAmout = whetherHeightScaleOnMouseWheel ? scrollAmount : 0;
		int widthScrollAmout = whetherWidthScaleOnMouseWheel ? scrollAmount : 0;
		pendingWheelHeightChange += heightScrollAmout;
		pendingWheelWidthChange += widthScrollAmout;
		pendingWheelPoint = e.getPoint();
		wheelZoomDebouncer.restart();
	}

	private void applyPendingWheelZoom() {
		int heightChange = pendingWheelHeightChange;
		int widthChange = pendingWheelWidthChange;
		Point point = pendingWheelPoint;
		pendingWheelHeightChange = 0;
		pendingWheelWidthChange = 0;
		pendingWheelPoint = null;
		if (point != null && (heightChange != 0 || widthChange != 0)) {
			phylogeneticTreePanel.continuouslyZoomInOrOut(heightChange, widthChange, point);
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {

	}

	@Override
	public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();
		List<GraphicsNode> selectedNodes = getTreeLayoutProperties().getSelectedNodes();

		if (selectedNodes.size() == 0) {
			return;
		}

		GraphicsNode node = selectedNodes.get(0);
		boolean isLeaf = node.getChildCount() == 0;
		boolean isRoot = node.getParentCount() == 0;

		GraphicsNode selectedNode = null;

		if (keyCode == KeyEvent.VK_UP) {
			if (!isRoot) {
				Optional<GraphicsNode> sibling = getSibling(node, true);
				if (sibling.isPresent()) {
					selectedNode = sibling.get();
				}
			}
		} else if (keyCode == KeyEvent.VK_DOWN) {
			if (!isRoot) {
				Optional<GraphicsNode> sibling = getSibling(node, false);
				if (sibling.isPresent()) {
					selectedNode = sibling.get();
				}
			}
		} else if (keyCode == KeyEvent.VK_RIGHT) {
			if (!isLeaf) {
				selectedNode = (GraphicsNode) node.getFirstChild();
			}
		} else if (keyCode == KeyEvent.VK_LEFT) {
			if (!isRoot) {
				selectedNode = (GraphicsNode) node.getParent();
			}
		}

		if (selectedNode == null) {
			return;
		}

		dealWithSelectedNodes(selectedNode);
		for (Consumer<GraphicsNode> consumer : tree2AnalyzingPanelInteractions) {
			consumer.accept(selectedNode);
		}

		String tooltipString = null;
		if (isLeaf) {
//			tooltipString = getTooltipString(getTreePopupMenu().getLeafTooltipInformation(selectedNode, false));
		} else {
//			tooltipString = getTooltipString(getTreePopupMenu().getInternalNodeTooltipInformation(selectedNode, false));
		}
		phylogeneticTreePanel.setToolTipText(tooltipString);
		phylogeneticTreePanel.refreshViewPort();
	}

	private Optional<GraphicsNode> getSibling(GraphicsNode node, boolean isUp) {
		GraphicsNode parent = (GraphicsNode) node.getParent();
		int indexOfChild = parent.indexOfChild(node);
		int childCount = parent.getChildCount();
		GraphicsNode ret = null;
		if (isUp) {
			if (indexOfChild > 0) {
				ret = (GraphicsNode) parent.getChildAt(indexOfChild - 1);
			}
		} else {
			if (indexOfChild < childCount - 1) {
				ret = (GraphicsNode) parent.getChildAt(indexOfChild + 1);
			}
		}

		return Optional.ofNullable(ret);
	}

	private JPopupMenu getPopupMenu4creativeMode() {
		List<GraphicsNode> selected = treeLayoutProperties.getSelectedNodes();
		return getPopupMenu4creativeMode(selected.isEmpty() ? null : selected.getFirst());
	}

	private JPopupMenu getPopupMenu4creativeMode(GraphicsNode target) {
		JPopupMenu popup = new JPopupMenu("Creative popup menu");
		boolean hasParent = target != null && target != rootNode && target.getParentCount() > 0;
		creativeItem(popup, "Add child", target, true, node -> {
			double length = node == rootNode ? 1 : node.getRealBranchLength();
			int count = node.getChildCount() == 0 ? 2 : 1;
			for (int i = 0; i < count; i++) node.addChild(newCreativeLeaf(length));
			phylogeneticTreePanel.completeTreeStructureChange();
		});
		creativeItem(popup, "Add sibling", target, hasParent, node -> {
			if (node.getParentCount() == 0) return;
			node.getParent().addChild(newCreativeLeaf(node.getRealBranchLength()));
			phylogeneticTreePanel.completeTreeStructureChange();
		});
		creativeItem(popup, "Add sibling and merge", target, hasParent, node -> {
			if (node.getParentCount() == 0) return;
			GraphicsNode parent = (GraphicsNode) node.getParent();
			double half = node.getRealBranchLength() / 2;
			GraphicsNode newParent = new GraphicsNode();
			newParent.setRealBranchLength(half);
			PhylogeneticTreePanel.replaceChild(parent, node, newParent);
			node.setRealBranchLength(half);
			newParent.addChild(node);
			newParent.addChild(newCreativeLeaf(half));
			phylogeneticTreePanel.completeTreeStructureChange();
		});
		creativeItem(popup, "Rename", target, true, node -> {
			Optional<String> name = SwingDialog.showInputDialog(phylogeneticTreePanel,
					node.getName() == null ? "" : node.getName());
			if (name.isPresent() && phylogeneticTreePanel.canEditTreeNode(node)) {
				node.setName(name.get());
				phylogeneticTreePanel.recalculatePreservingZoom();
			}
		});
		creativeItem(popup, "Swap with sibling", target,
				hasParent && target.getParent().getChildCount() > 1, node -> {
			if (node.getParentCount() == 0 || node.getParent().getChildCount() < 2) return;
			EvolNodeUtil.swapNodeWithSibling(node);
			phylogeneticTreePanel.completeTreeStructureChange();
		});
		for (boolean up : new boolean[] {true, false}) {
			creativeItem(popup, up ? "Ladderize up" : "Ladderize down", target,
					target != null && target.getChildCount() > 0, node -> {
				EvolNodeUtil.initializeSize(node);
				EvolNodeUtil.ladderizeNodeAccording2sizeAndLength(node, up);
				phylogeneticTreePanel.completeTreeStructureChange();
			});
		}
		creativeItem(popup, "Delete clade", target, phylogeneticTreePanel.canRemoveSubtree(target),
				node -> phylogeneticTreePanel.removeSubtree(node, true));
		return popup;
	}

	private GraphicsNode newCreativeLeaf(double length) {
		GraphicsNode leaf = new GraphicsNode();
		leaf.setName("Leaf_" + leaf.getID());
		leaf.setRealBranchLength(length);
		leaf.getDrawUnit().setDrawName(treeLayoutProperties.getShowLeafPropertiesInfo().isShowLeafLabel());
		return leaf;
	}

	private void creativeItem(JPopupMenu popup, String name, GraphicsNode target, boolean applicable,
			Consumer<GraphicsNode> action) {
		JMenuItem item = new JMenuItem(name);
		item.setFont(UnifiedAccessPoint.getLaunchProperty().getDefaultFont());
		item.setEnabled(applicable && treeLayoutProperties.isCreativeMode() && phylogeneticTreePanel.canEditTreeNode(target));
		item.setToolTipText(switch (name) {
			case "Add child" -> "Add a child. A leaf becomes an internal node with two new leaves.";
			case "Add sibling" -> "Add a sibling under this node's parent. The root has no sibling.";
			case "Add sibling and merge" -> "Insert a parent and new sibling, splitting this node's incoming branch without changing existing leaf distances.";
			case "Rename" -> "Edit this node's stored name and exported label.";
			case "Swap with sibling" -> "Swap this node with an adjacent sibling without changing branch lengths.";
			case "Delete clade" -> "Confirm removal of this subtree. Contract non-root unary parents and preserve surviving leaf distances.";
			default -> "Order this clade's descendants without changing topology or branch lengths.";
		});
		item.addActionListener(event -> {
			if (applicable && treeLayoutProperties.isCreativeMode() && phylogeneticTreePanel.canEditTreeNode(target)) action.accept(target);
		});
		popup.add(item);
	}

	private TreePopupMenu getTreePopupMenu() {
		if (jPopupMenu4normalMode == null) {
			jPopupMenu4normalMode = new TreePopupMenu(phylogeneticTreePanel);
		}
		return jPopupMenu4normalMode;
	}

	public Rectangle getRect4selectArea() {
		return rect4selectArea;
	}

	public List<Consumer<GraphicsNode>> getTree2AnalyzingPanelInteractions() {
		return tree2AnalyzingPanelInteractions;
	}
}
