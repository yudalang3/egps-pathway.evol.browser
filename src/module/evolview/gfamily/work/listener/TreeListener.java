package module.evolview.gfamily.work.listener;

import egps2.UnifiedAccessPoint;
import egps2.panels.dialog.SwingDialog;
import evoltree.struct.EvolNode;
import evoltree.struct.util.EvolNodeUtil;
import evoltree.txtdisplay.TreeDrawUnit;
import module.evolview.gfamily.work.gui.ScaleBarRectObject;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.gfamily.work.gui.tree.RectObj;
import module.evolview.gfamily.work.gui.tree.TreePopupMenu;
import module.evolview.common.SwingDebouncer;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.graphics.struct.TreeDecideUtil;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger logger = LoggerFactory.getLogger(TreeListener.class);

	private static final long TIME_DIFFERENCE_FOR_IDLE = 300;
	private long lastMoveTime;

	private final TreeLayoutProperties treeLayoutProperties;
	private final List<Consumer<GraphicsNode>> tree2AnalyzingPanelInteractions = Lists.newArrayList();

	private GraphicsNode rootNode;
	private final PhylogeneticTreePanel phylogeneticTreePanel;

	private JPopupMenu jPopupMenu4creativeMode;
	private TreePopupMenu jPopupMenu4normalMode;

	private final Line2D.Double repeatLine = new Line2D.Double();

	private RectObj scaleBarRectAdjust;
	private boolean whetherScaleBarSelectedInDrag = false;

	private Point origin;
	private boolean isDragging = false;
	private enum Gesture { NONE, SELECT, PAN, EDIT, SCALE }
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
			case SCALE -> {
				getScaleBarListenerObj().adjustPaintings(arg0.getX(), arg0.getY());
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

		if (phylogeneticTreePanel.supportsScaleBar() && getTreeLayoutProperties().isShowScaleBar()) {
			boolean contains = getScaleBarListenerObj().contains(point.getX(), point.getY());
			if (contains) {
				component.setCursor(Cursor.getPredefinedCursor(getScaleBarListenerObj().getCursorType()));
				return;
			} else {
				component.setCursor(Cursor.getDefaultCursor());
			}
		}

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
		} else if (gesture != Gesture.NONE && gesture != Gesture.SCALE) {
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
		whetherScaleBarSelectedInDrag = false;
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
		if (treeLayoutProperties.isShowScaleBar() && phylogeneticTreePanel.supportsScaleBar()) {
			Point point = arg0.getPoint();
			boolean contains = getScaleBarListenerObj().contains(point.getX(), point.getY());
			if (contains) {
				getScaleBarListenerObj().setFirstPressedPoint(point.x, point.y);
			}
			this.whetherScaleBarSelectedInDrag = contains;
		}
		if (whetherScaleBarSelectedInDrag) {
			gesture = Gesture.SCALE;
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

					boolean isLeaf = TreeDecideUtil.decideNodeTypeOfLeaf(selectedNode);
					// 在进行filter后内节点可能变成叶子节点，所以不能单通过判断是否有child判断叶子，加上了name是否为空的判断
					if (isLeaf && !selectedNode.getName().isEmpty()) {
					} else {
						getTreePopupMenu().collapseThisNode(selectedNode);
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
				JPopupMenu popup = getPopupMenu4creativeMode();
				popup.show(component, e.getX(), e.getY());
			} else {
				boolean isLeaf = TreeDecideUtil.decideNodeTypeOfLeaf(selectedNode);
				// 在进行filter后内节点可能变成叶子节点，所以不能单通过判断是否有child判断叶子，加上了name是否为空的判断
				if (isLeaf && !selectedNode.getName().isEmpty()) {
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
			dealWithSelectedNodes(null);
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

		double xSelf = node.getXSelf();
		double ySelf = node.getYSelf();

		double distanceSq = point.distanceSq(xSelf, ySelf);

//		logger.info("The dis is {}", distanceSq);
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
		if (jPopupMenu4creativeMode == null) {
			jPopupMenu4creativeMode = new JPopupMenu("Creative popup menu");

			Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();

			{
				JMenuItem jMenuItem = new JMenuItem("Add child");
				jPopupMenu4creativeMode.add(jMenuItem);
				jMenuItem.setFont(defaultFont);
				jMenuItem.addActionListener(e -> {
					if (!treeLayoutProperties.isCreativeMode()) return;
					List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
					if (selectedNodes.isEmpty()) {
						return;
					}
					GraphicsNode graphicsNode = selectedNodes.get(0);
					double realBranchLength = graphicsNode.getRealBranchLength();
					if (graphicsNode.getChildCount() == 0) {

						GraphicsNode graphicsNode2 = new GraphicsNode();
						String name = "Leaf_" + GraphicsNode.nextID;
						graphicsNode2.setName(name);
						GraphicsNode graphicsNode3 = new GraphicsNode();
						name = "Leaf_" + GraphicsNode.nextID;
						graphicsNode3.setName(name);

						graphicsNode.addChild(graphicsNode2);
						graphicsNode.addChild(graphicsNode3);
						graphicsNode2.setRealBranchLength(realBranchLength);
						graphicsNode3.setRealBranchLength(realBranchLength);

					} else {
						GraphicsNode graphicsNode2 = new GraphicsNode();
						String name = "Leaf_" + GraphicsNode.nextID;
						graphicsNode2.setName(name);
						graphicsNode2.setRealBranchLength(realBranchLength);
						graphicsNode.addChild(graphicsNode2);
					}

					getTreeLayoutProperties().initializeRoot();
					phylogeneticTreePanel.updatePhylogeneticTreePanel();
				});
			}

			{
				JMenuItem jMenuItem = new JMenuItem("Add sibling");
				jPopupMenu4creativeMode.add(jMenuItem);
				jMenuItem.setFont(defaultFont);
				jMenuItem.addActionListener(e -> {
					if (!treeLayoutProperties.isCreativeMode()) return;
					List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
					if (selectedNodes.isEmpty()) {
						return;
					}
					GraphicsNode graphicsNode = selectedNodes.get(0);
					double realBranchLength = graphicsNode.getRealBranchLength();

					EvolNode parent = graphicsNode.getParent();
					GraphicsNode graphicsNode2 = new GraphicsNode();
					String name = "Leaf_" + GraphicsNode.nextID;
					graphicsNode2.setName(name);
					graphicsNode2.setRealBranchLength(realBranchLength);
					parent.addChild(graphicsNode2);

					getTreeLayoutProperties().initializeRoot();
					phylogeneticTreePanel.updatePhylogeneticTreePanel();
				});
			}
			{
				JMenuItem jMenuItem = new JMenuItem("Add sibling and merge");
				jPopupMenu4creativeMode.add(jMenuItem);
				jMenuItem.setFont(defaultFont);
				jMenuItem.addActionListener(e -> {
					if (!treeLayoutProperties.isCreativeMode()) return;
					List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
					if (selectedNodes.isEmpty()) {
						return;
					}
					GraphicsNode graphicsNode = selectedNodes.get(0);
					double realBranchLength = graphicsNode.getRealBranchLength();

					EvolNode parent = graphicsNode.getParent();
					GraphicsNode graphicsNode2 = new GraphicsNode();
					String name = "Leaf_" + GraphicsNode.nextID;
					graphicsNode2.setName(name);
					graphicsNode2.setRealBranchLength(realBranchLength);

					graphicsNode.removeAllParent();

					GraphicsNode newParent = new GraphicsNode();
					newParent.setRealBranchLength(realBranchLength);
					newParent.addChild(graphicsNode2);
					newParent.addChild(graphicsNode);

					parent.addChild(newParent);

					getTreeLayoutProperties().initializeRoot();
					phylogeneticTreePanel.updatePhylogeneticTreePanel();
				});
			}
			{
				JMenuItem jMenuItem = new JMenuItem("Rename");
				jPopupMenu4creativeMode.add(jMenuItem);
				jMenuItem.setFont(defaultFont);
				jMenuItem.addActionListener(e -> {
					if (!treeLayoutProperties.isCreativeMode()) return;
					List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
					if (selectedNodes.isEmpty()) {
						return;
					}
					GraphicsNode graphicsNode = selectedNodes.get(0);

					String name = graphicsNode.getName();
					name = name == null ? "" : name;
					Optional<String> showInputDialog = SwingDialog.showInputDialog(UnifiedAccessPoint.getInstanceFrame(),
							name);
					if (showInputDialog.isPresent()) {
						graphicsNode.setName(showInputDialog.get());

						getTreeLayoutProperties().initializeRoot();

						TreeLayout myLayout = treeLayoutProperties.getMyLayout();
						getTreeLayoutProperties().initializeRoot();
						phylogeneticTreePanel.updateViewAccording2TreeLayout(myLayout);
					}

				});
			}

			{
				JMenuItem jMenuItem = new JMenuItem("Swap with sibling");
				jPopupMenu4creativeMode.add(jMenuItem);
				jMenuItem.setFont(defaultFont);
				jMenuItem.addActionListener(e -> {
					if (!treeLayoutProperties.isCreativeMode()) return;
					List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
					if (selectedNodes.isEmpty()) {
						return;
					}
					GraphicsNode graphicsNode = selectedNodes.get(0);
					if (graphicsNode.getParentCount() == 0) {
						return;
					}

					EvolNodeUtil.swapNodeWithSibling(graphicsNode);
					phylogeneticTreePanel.updatePhylogeneticTreePanel();
				});
			}

			{
				JMenuItem jMenuItem = new JMenuItem("Ladderize up");
				jPopupMenu4creativeMode.add(jMenuItem);
				jMenuItem.setFont(defaultFont);
				jMenuItem.addActionListener(e -> {
					if (!treeLayoutProperties.isCreativeMode()) return;
					List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
					if (selectedNodes.isEmpty()) {
						return;
					}
					GraphicsNode node = selectedNodes.get(0);
					EvolNodeUtil.initializeSize(node);
					EvolNodeUtil.ladderizeNodeAccording2sizeAndLength(node, true);
					phylogeneticTreePanel.updatePhylogeneticTreePanel();
				});
			}

			{
				JMenuItem jMenuItem = new JMenuItem("Ladderize down");
				jPopupMenu4creativeMode.add(jMenuItem);
				jMenuItem.setFont(defaultFont);
				jMenuItem.addActionListener(e -> {
					if (!treeLayoutProperties.isCreativeMode()) return;
					List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
					if (selectedNodes.isEmpty()) {
						return;
					}
					GraphicsNode node = selectedNodes.get(0);
					EvolNodeUtil.initializeSize(node);
					EvolNodeUtil.ladderizeNodeAccording2sizeAndLength(node, false);
					phylogeneticTreePanel.updatePhylogeneticTreePanel();
				});
			}
			{
				JMenuItem jMenuItem = new JMenuItem("Delete clade");
				jPopupMenu4creativeMode.add(jMenuItem);
				jMenuItem.setFont(defaultFont);
				jMenuItem.addActionListener(e -> {
					if (!treeLayoutProperties.isCreativeMode()) return;
					List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();
					if (selectedNodes.isEmpty()) {
						return;
					}
					GraphicsNode node = selectedNodes.get(0);

					Optional<GraphicsNode> parentOpt = EvolNodeUtil.getParent(node);
					if (!parentOpt.isPresent()) {
						return;
					}
					GraphicsNode parent = parentOpt.get();
					int childCount = parent.getChildCount();

					if (childCount > 2) {
						node.removeAllParent();
						phylogeneticTreePanel.updatePhylogeneticTreePanel();
						return;
					}
					Optional<GraphicsNode> grandParentOpt = EvolNodeUtil.getParent(parent);
					if (!grandParentOpt.isPresent()) {
						SwingDialog.showErrorMSGDialog("Delete error",
								"Can not delete this clade, \nbecause the parent is a binary fork while the grand parent not exist.");
						return;
					}

					GraphicsNode grandParent = grandParentOpt.get();
					GraphicsNode[] childrenArray = EvolNodeUtil.getChildrenArray(parent);
					parent.removeAllChild();
					parent.removeAllParent();

					for (GraphicsNode iter : childrenArray) {
						if (iter != node) {
							grandParent.addChild(iter);
						}
					}
					phylogeneticTreePanel.updatePhylogeneticTreePanel();

				});
			}

		}

		for (Component component : jPopupMenu4creativeMode.getComponents()) if (component instanceof JMenuItem item) {
			item.setToolTipText(switch (item.getText()) {
				case "Add child" -> "Add a child to the selected node. A selected leaf becomes an internal node with two new leaves. Changes tree topology.";
				case "Add sibling" -> "Add a new node under the selected node's parent. Changes tree topology; the root has no sibling.";
				case "Add sibling and merge" -> "Group the selected node and a new sibling under a new parent. Changes tree topology.";
				case "Rename" -> "Edit the selected node's stored name. Updates labels and exported tree data.";
				case "Swap with sibling" -> "Exchange the displayed order of the selected node and a sibling. Keep branch lengths.";
				case "Ladderize up" -> "Order descendants of the selected clade in ascending ladder order. Keep topology and branch lengths.";
				case "Ladderize down" -> "Order descendants of the selected clade in descending ladder order. Keep topology and branch lengths.";
				case "Delete clade" -> "Delete the selected node and all descendants from the tree. The root cannot be deleted.";
				default -> item.getText();
			});
		}
		return jPopupMenu4creativeMode;
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
