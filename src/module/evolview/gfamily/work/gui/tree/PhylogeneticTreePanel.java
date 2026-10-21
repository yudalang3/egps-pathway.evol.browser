package module.evolview.gfamily.work.gui.tree;

import egps2.UnifiedAccessPoint;
import egps2.frame.gui.EGPSMainGuiUtil;
import egps2.panels.dialog.SwingDialog;
import egps2.panels.dialog.modern.ModernDialog;
import egps2.panels.dialog.modern.ModernDialogType;
import graphic.engine.guicalculator.BlankArea;
import module.evolview.gfamily.work.listener.TreeListener;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.model.tree.ScaleBarProperty;
import module.evolview.phylotree.visualization.annotation.AnnotationsProperties;
import module.evolview.phylotree.visualization.annotation.AnnotationsProperties4LinageType;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CancellationException;

/**
 * This class is the "Phylogenetic Tree" tab.
 */
public class PhylogeneticTreePanel extends JPanel implements TreeLayoutHost {
	private static final Logger logger = LoggerFactory.getLogger(PhylogeneticTreePanel.class);

	protected TreeLayoutProperties layoutProp;

	private int currentHeight;
	private int currentWidth;
	private Dimension fitCanvasSize = new Dimension();
	private Boolean spiralLeafLabels;
	private Boolean otherLeafLabels;
	private boolean spiralFamily;

	protected BaseLayout layout;
	private final EnumMap<TreeLayout, BaseLayout> unrootedLayouts = new EnumMap<>(TreeLayout.class);
	private SwingWorker<UnrootedLayoutCalculator.Result, Void> layoutWorker;
	private long layoutRevision;
	private boolean layoutReady;
	private String layoutStatus = "Calculating tree layout...";

	/**
	 * 在 PhylogeneticTreePanel有一个是否显示scaleBar的属性。那个是控制全局是否显示scale bar的。
	 */
	private final ScaleBarProperty scaleBarProperty = new ScaleBarProperty();

	private AnnotationsProperties mainAnnotationsProperties;
	private AnnotationsProperties4LinageType linageTypeAnnotationsProperties;

	protected TreeListener treeListener;
	protected GraphicsNode rootNode;

	private boolean isGlobalPhylogeneticTreePanel = false;

	private final Color color2fill4selectedArea = new Color(0, 0, 255, 50);

	public PhylogeneticTreePanel(TreeLayoutProperties properties) {
		this(properties, properties.getOriginalRootNode(), null, null);
	}

	public PhylogeneticTreePanel(TreeLayoutProperties treePro, GraphicsNode node,
			AnnotationsProperties4LinageType linageTypeAnnotationsProperties,
			AnnotationsProperties mainAnnotationsProperties) {
		setLayout(new BorderLayout());

		this.rootNode = node;
		this.layoutProp = treePro;
		BranchLengthDomain.maximum(node, false);

		setAutoscrolls(true);
		initializeTreeListener();

		setBackground(Color.white);

		if (linageTypeAnnotationsProperties == null) {
			this.linageTypeAnnotationsProperties = new AnnotationsProperties4LinageType();
		} else {
			this.linageTypeAnnotationsProperties = linageTypeAnnotationsProperties;
		}

		if (mainAnnotationsProperties == null) {
			this.mainAnnotationsProperties = new AnnotationsProperties();
		} else {
			this.mainAnnotationsProperties = mainAnnotationsProperties;
		}

	}

	private void initializeTreeListener() {
		treeListener = new TreeListener(layoutProp, rootNode, this);
		addMouseMotionListener(treeListener);
		addMouseListener(treeListener);
		addMouseWheelListener(treeListener);
		addKeyListener(treeListener);

	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (layout == null) {
			EGPSMainGuiUtil.drawLetUserImportDataString((Graphics2D) g);
			return;
		}
		if (!layoutReady) {
			g.drawString(layoutStatus, 20, 30);
			return;
		}

		Graphics2D g2d = (Graphics2D) g.create();
		try {
			EGPSMainGuiUtil.setupHighQualityRendering(g2d);
			drawProcess(g2d);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			g2d.dispose();
		}

	}

	protected void drawProcess(Graphics2D g2d) {
		/*
		  为什么这里是缩小才会重新计算呢？
		  因为如果改成放大或者缩小都计算的话，那么1)查看tool tip时候，2)鼠标右击的时候 都是重新refresh，所以先只是放大！
		 */
		if (currentHeight != 0) {
			if (currentHeight < getHeight() || currentWidth < getWidth()) {
				updatePhylogeneticTreePanel4fitFrame();
				return;
			}
		}

        if (layoutProp.isCreativeMode()) {
			Image image = layoutProp.getBackgroundImage4CreativeMode();
			BlankArea blankArea = layoutProp.getBlankArea();
			if (image != null) {
				int workWidth = blankArea.getWorkWidth(getWidth());
				int workHeight = blankArea.getWorkHeight(getHeight());
				g2d.drawImage(image, blankArea.getLeft(), blankArea.getTop(), workWidth, workHeight, null);
			}

		}


		layout.paintGraphics(g2d);
		layout.drawFixedTitle(g2d, getWidth(), getHeight());

		if (layoutProp.isShowWidthAndHeightString()) {
			String format = String.format("Width: %d / height: %d", currentWidth, currentHeight);
			Font axisFont = layoutProp.getAxisFont();
			g2d.setFont(axisFont);
			g2d.drawString(format, 30, 30);

			BlankArea blankArea = layout.getBlankArea();
			String string = blankArea.toString();
			g2d.drawString(string, 30, 45);
		}

		Rectangle rect4selectArea = treeListener.getRect4selectArea();
		if (rect4selectArea.getWidth() > 0) {
			// 设置颜色和透明度
			g2d.setColor(color2fill4selectedArea); // 半透明蓝色
			g2d.fill(rect4selectArea); // 填充矩形区域

			g2d.setColor(Color.BLUE); // 边框颜色
			g2d.draw(rect4selectArea); // 绘制矩形边框
		}

	}

	/**
	 * 这是比较弱的更新方法
	 */
	public void refreshViewPort() {
		SwingUtilities.invokeLater(() -> {
			repaint();
			this.revalidate();
		});
	}

	/**
	 * 这是最弱的更新方法
	 */
	public void weakRefreshViewPort() {
		SwingUtilities.invokeLater(this::repaint);
	}

	public void updatePhylogeneticTreePanel4fitFrame() {
		fitCanvasSize = new Dimension();

		Container parent = this.getParent();
		if (parent instanceof JViewport viewPort) {

            currentWidth = viewPort.getWidth();
			currentHeight = viewPort.getHeight();
		} else {
			currentWidth = getWidth();
			currentHeight = getHeight();

			SwingDialog.showWarningMSGDialog("Warning to developers",
					"The tree panel should be add in the JScollPanel.");

		}

		updatePhylogeneticTreePanel();
	}

	/**
	 * 
	 */
	public void updatePhylogeneticTreePanel() {
		if (UnifiedAccessPoint.isGULaunched() && !SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(this::updatePhylogeneticTreePanel);
			return;
		}
		boolean wasPending = isLayoutCalculationPending();
		long revision = ++layoutRevision;
		layoutStatus = "Calculating tree layout...";
		if (layoutWorker != null) {
			layoutWorker.cancel(true);
			layoutWorker = null;
		}
		if (layout == null) {
			firePropertyChange("layoutCalculationPending", wasPending, false);
			return;
		}
		try {
			TreeLayout resolved = resolveDistanceLayout(layoutProp.getMyLayout());
			if (resolved != layoutProp.getMyLayout()) {
				layoutProp.setMyLayout(resolved);
				layout = obtainRightLayout(resolved);
				spiralFamily = false;
			}
		} catch (IllegalArgumentException error) {
			layoutReady = false;
			layoutStatus = error.getMessage();
			logger.warn("Unable to establish a distance layout: {}", error.getMessage());
			notifyTreeViewStateChanged();
			if (UnifiedAccessPoint.isGULaunched()) SwingDialog.showWarningMSGDialog("Tree layout", error.getMessage());
			return;
		}
		layout.prepareLabelSpace();
		Dimension canvas = layout.getRequiredCanvasSize(currentWidth, currentHeight);
		if (fitCanvasSize.width <= 0 || fitCanvasSize.height <= 0) fitCanvasSize = new Dimension(canvas);
		currentWidth = canvas.width;
		currentHeight = canvas.height;
		setPreferredSize(canvas);
		firePropertyChange("treePanelDimension", null, new Dimension(canvas));
		if (layout instanceof RadialPhyloLayout radial && SwingUtilities.isEventDispatchThread()) {
			RadialPhyloLayout.Calculation request;
			try {
				request = radial.prepareCalculation();
			} catch (IllegalArgumentException ex) {
				layoutReady = false;
				layoutStatus = "Unable to calculate the tree layout.";
				firePropertyChange("layoutCalculationPending", wasPending, false);
				notifyTreeViewStateChanged();
				SwingDialog.showWarningMSGDialog("Tree layout", ex.getMessage());
				return;
			}
			if (request.cached() != null) {
				radial.applyCalculation(request, request.cached(), canvas.width, canvas.height);
				layoutReady = true;
				firePropertyChange("layoutCalculationPending", wasPending, false);
				notifyTreeViewStateChanged();
				refreshViewPort();
				return;
			}
			layoutReady = false;
			layoutWorker = new SwingWorker<>() {
				@Override protected UnrootedLayoutCalculator.Result doInBackground() {
					return radial.compute(request);
				}
				@Override protected void done() {
					if (revision != layoutRevision || radial != layout || isCancelled()) return;
					try {
						radial.applyCalculation(request, get(), currentWidth, currentHeight);
						layoutReady = true;
					} catch (CancellationException ignored) {
						return;
					} catch (Exception ex) {
						layoutStatus = "Unable to calculate the tree layout.";
						logger.error("Unable to calculate unrooted tree layout", ex);
						SwingDialog.showWarningMSGDialog("Tree layout", "Unable to calculate the tree layout. See the log for details.");
					} finally {
						layoutWorker = null;
						firePropertyChange("layoutCalculationPending", true, false);
					}
					notifyTreeViewStateChanged();
					refreshViewPort();
				}
			};
			layoutWorker.execute();
			firePropertyChange("layoutCalculationPending", wasPending, true);
			repaint();
		} else {
			// These are linear coordinate mappings. Publish them together on the EDT.
			layout.calculateForPainting(currentWidth, currentHeight);
			layoutReady = true;
			firePropertyChange("layoutCalculationPending", wasPending, false);
			refreshViewPort();
		}
		notifyTreeViewStateChanged();
	}

	/** Explicit cancellation leaves the last completed coordinates available for the next request. */
	public void cancelLayoutCalculation() {
		if (!isLayoutCalculationPending()) return;
		++layoutRevision;
		if (layoutWorker != null) layoutWorker.cancel(true);
		layoutWorker = null;
		firePropertyChange("layoutCalculationPending", true, false);
		notifyTreeViewStateChanged();
		layoutStatus = "Layout calculation cancelled. Choose a layout to recalculate.";
		repaint();
	}

	public boolean isLayoutCalculationPending() {
		return layoutWorker != null;
	}

	/**
	 * 第一次启动本程序之后的调用方法。
	 */
	public void initializeLeftPanel() {
		this.isGlobalPhylogeneticTreePanel = true;

		// Use the layout from TreeLayoutProperties (may be set by parameters)
		// If not set, default is RECT_PHYLO_LEFT
		TreeLayout initialLayout = layoutProp.getMyLayout();
		if (initialLayout == null) {
			initialLayout = TreeLayout.RECT_PHYLO_LEFT;
			layoutProp.setMyLayout(initialLayout);
		}
		initialLayout = resolveDistanceLayout(initialLayout);
		layoutProp.setMyLayout(initialLayout);
		spiralFamily = initialLayout.name().startsWith("SPRIAL_");
		otherLeafLabels = layoutProp.getShowLeafPropertiesInfo().isShowLeafLabel();
		if (spiralFamily) spiralLeafLabels = otherLeafLabels;
		layout = obtainRightLayout(initialLayout);

		updatePhylogeneticTreePanel4fitFrame();
	}

	public void updateViewAccording2TreeLayout(TreeLayout tt) {
		tt = resolveDistanceLayout(tt);
		boolean enteringSpiral = tt.name().startsWith("SPRIAL_");
		if (enteringSpiral != spiralFamily) {
			boolean current = layoutProp.getShowLeafPropertiesInfo().isShowLeafLabel();
			if (spiralFamily) spiralLeafLabels = current; else otherLeafLabels = current;
			spiralFamily = enteringSpiral;
			setLeafLabelsVisible(enteringSpiral ? Boolean.TRUE.equals(spiralLeafLabels)
					: otherLeafLabels == null ? current : otherLeafLabels);
		}
		layoutProp.setMyLayout(tt);
		layout = obtainRightLayout(tt);
		layoutReady = false;
		recalculatePreservingZoom();
	}

	private TreeLayout resolveDistanceLayout(TreeLayout requested) {
		if (requested.isCladogram()) return requested;
		if (BranchLengthDomain.maximum(rootNode, true) > 0) return requested;
		String message = "No positive branch-length range is available. Showing a rectangular cladogram; stored branch lengths are unchanged.";
		logger.info(message);
		if (UnifiedAccessPoint.isGULaunched()) SwingDialog.showWarningMSGDialog("Tree layout", message);
		return TreeLayout.RECT_CLADO_ALIGNED_LEFT;
	}

	public void recalculatePreservingZoom() {
		if (layout == null) return;
		Dimension scale = getTreePanelScale();
		Container parent = getParent();
		if (parent instanceof JViewport viewport) {
			layout.prepareLabelSpace();
			fitCanvasSize = layout.getRequiredCanvasSize(viewport.getWidth(), viewport.getHeight());
			if (isUniformScaleLayout()) scale.width = scale.height = Math.max(scale.width, scale.height);
			currentWidth = (int) Math.round(fitCanvasSize.width * scale.width / 100.0);
			currentHeight = (int) Math.round(fitCanvasSize.height * scale.height / 100.0);
		}
		updatePhylogeneticTreePanel();
	}

	/**
	 * 注意这里还有一项功能是 controller.getLeftOparetionPanel().enableScaleBarStates(enable);
	 *
	 * @param currLayout
	 */
	private BaseLayout obtainRightLayout(TreeLayout currLayout) {
		BaseLayout cached = unrootedLayouts.get(currLayout);
		if (cached != null) {
			cached.setRootNode(rootNode);
			return cached;
		}
		TreeLayoutProperties treeLayoutProperties = layoutProp;
		BaseLayout ret;
		switch (currLayout) {
		case SLOPE_CLADO_ALIGNED_LEFT:
			ret = new SlopeCladoAlignedLeft(treeLayoutProperties, rootNode, this);
			break;
		case RECT_CLADO_ALIGNED_LEFT:
			ret = new RectCladoAlign2tip(treeLayoutProperties, rootNode, this);
			break;
		case RECT_CLADO_EQUAL_LEFT:
			ret = new RectCladoEqual(treeLayoutProperties, rootNode, this);
			break;
		case RECT_INNER_YAXIS:
			ret = new RectPhyloInnerYAxis(treeLayoutProperties, rootNode, this);
			break;
		case RECT_HALF_CIRCLE:
			ret = new RectHalfCircleLayout(treeLayoutProperties, rootNode, this);
			break;
		case RECT_PHYLO_ALIGN2TIP:
			ret = new RectPhyloAlign2Tip(treeLayoutProperties, rootNode, this);
			break;
		case RECT_PHYLO_LEFT:
			ret = new RectPhyloLayout(treeLayoutProperties, rootNode, this);
			break;
		case INNER_CIR_CLADO:
			ret = new CircularInnerCladoAligned(treeLayoutProperties, rootNode, this);
			break;
		case OUTER_CIR_CLADO_ALIGNED:
			ret = new CircularCladoAlign2Tip(treeLayoutProperties, rootNode, this);
			break;
		case OUTER_CIR_CLADO_EQUAL:
			ret = new CircularCladoEqual(treeLayoutProperties, rootNode, this);
			break;
		case OUTER_CIR_PHYLO:
			ret = new CircularPhylo(treeLayoutProperties, rootNode, this);
			break;
		case OUTER_CIR_PHYLO_ALIGNED:
			ret = new CircularPhyloAlign2Tip(treeLayoutProperties, rootNode, this);
			break;
		case RADIAL_CLADO:
			ret = new RadialCladoLayout(treeLayoutProperties, rootNode, this);
			break;
		case RADIAL_EQUAL_DAYLIGHT_PHYLO:
			ret = new RadicalEqualDaylightPhyloLayout(treeLayoutProperties, rootNode, this);
			break;
		case RADIAL_EQUAL_DAYLIGHT_CLADO:
			ret = new RadicalEqualDaylightCladoLayout(treeLayoutProperties, rootNode, this);
			break;
		case RADIAL_NBODY_PHYLO:
			ret = new RadialNBodyLayout(treeLayoutProperties, rootNode, this, false);
			break;
		case RADIAL_NBODY_CLADO:
			ret = new RadialNBodyLayout(treeLayoutProperties, rootNode, this, true);
			break;
		case SPRIAL_ALPHA_PHYLO:
			ret = new SpiralPhyloWithAlpha(treeLayoutProperties, rootNode, this);
			break;
		case SPRIAL_ALPHA_CLADO:
			ret = new SprialCladoAlignedWithAlpha(treeLayoutProperties, rootNode, this);
			break;
		case SPRIAL_BETA_PHYLO:
			ret = new SpiralPhyloWithBeta(treeLayoutProperties, rootNode, this);
			break;
		case SPRIAL_BETA_CLADO:
			ret = new SpiralCladoAlignedWithBeta(treeLayoutProperties, rootNode, this);
			break;
		default:
			// RADIAL_PHYLO
			ret = new RadialPhyloLayout(treeLayoutProperties, rootNode, this);
			break;
		}
		if (ret instanceof RadialPhyloLayout) unrootedLayouts.put(currLayout, ret);
		return ret;
	}

	private void recalculatedInstructions(int w, int h) {
		currentWidth = w;
		currentHeight = h;
		updatePhylogeneticTreePanel();

	}

	/**
	 * 鼠标滚轮的滑动！
	 * 
	 * @param heightChange
	 * @param widthChange
	 * @param p
	 */
	public void continuouslyZoomInOrOut(int heightChange, int widthChange, Point p) {

		if (currentHeight <= 0 || currentWidth <= 0) return;
		int tempHeight = Math.max(fitCanvasSize.height, currentHeight + heightChange);
		int tempWidth = Math.max(fitCanvasSize.width, currentWidth + widthChange);
		if (isUniformScaleLayout()) {
			double factor = heightChange != 0 ? tempHeight / (double) currentHeight : tempWidth / (double) currentWidth;
			factor = Math.max(factor, Math.max(fitCanvasSize.height / (double) currentHeight, fitCanvasSize.width / (double) currentWidth));
			tempHeight = (int) Math.round(currentHeight * factor);
			tempWidth = (int) Math.round(currentWidth * factor);
		}
		double d1 = tempHeight / (double) (currentHeight);
		double d2 = tempWidth / (double) (currentWidth);

		currentHeight = tempHeight;
		currentWidth = tempWidth;
		recalculatedInstructions(currentWidth, currentHeight);

		// The left control panel should update the value
		Point location = getLocation();

		int offX = (int) (p.x * d2) - p.x;
		int offY = (int) (p.y * d1) - p.y;

		setLocation(location.x - offX, location.y - offY);

		logger.trace("The mouse point location is {} {}", p.getX(), p.getY());
		logger.trace("Current location is {} {}", location.getX(), location.getY());
		logger.trace("The new location is {} {}", location.x - offX, location.y - offY);

		Container parent = getParent();
		if (parent != null) parent.doLayout();
		/*
		 * 终于解决这个问题了。
		 * https://stackoverflow.com/questions/115103/how-do-you-implement-position-
		 * sensitive-zooming-inside-a-jscrollpane 之前是用这个
		 * revalidate()，但是这种方式一直有一个问题：第一次直接 zoom in at this node的时候，一直在左上角。
		 * this.revalidate();
		 */

	}

	public void discreteZoomInOrOut(int heightChange, int widthChange) {
		List<GraphicsNode> selectedNodes = layoutProp.getSelectedNodes();
		if (selectedNodes.size() > 0) {
			GraphicsNode node = selectedNodes.get(selectedNodes.size() - 1);
			double xSelf = node.getXSelf();
			double ySelf = node.getYSelf();

			Point point = new Point();
			point.setLocation(xSelf, ySelf);
			continuouslyZoomInOrOut(heightChange, widthChange, point);

			// Scroll to center the selected node in the viewport
			SwingUtilities.invokeLater(() -> scrollNodeToCenter(node));
		} else {
			SwingDialog.showInfoMSGDialog("Choose node first", "Your need to choose node first");
		}

	}

	/**
	 * Scroll the viewport so that the specified node is centered.
	 *
	 * @param node the node to center in the viewport
	 */
	public void scrollNodeToCenter(GraphicsNode node) {
		JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
		if (viewport == null) {
			return;
		}

		int nodeX = (int) node.getXSelf();
		int nodeY = (int) node.getYSelf();

		int viewWidth = viewport.getWidth();
		int viewHeight = viewport.getHeight();

		// Calculate target position to center the node
		int targetX = Math.max(0, nodeX - viewWidth / 2);
		int targetY = Math.max(0, nodeY - viewHeight / 2);

		viewport.setViewPosition(new Point(targetX, targetY));
	}

	public TreeLayoutProperties getLayoutProperties() {
		return layoutProp;
	}

	public void resetNewRoot(GraphicsNode newRoot) {
		cancelLayoutCalculation();
		this.rootNode = newRoot;
		layout.setRootNode(newRoot);
		treeListener.setRootNode(newRoot);
	}

	public void updateViewAccording2TreeLayoutForAnnotationDialog() {
		layout = obtainRightLayout(TreeLayout.RECT_CLADO_ALIGNED_LEFT);
		actionsForAnnotationDialog();
		updatePhylogeneticTreePanel();

	}

	private void actionsForAnnotationDialog() {
		removeMouseMotionListener(treeListener);
		removeMouseWheelListener(treeListener);
		removeMouseListener(treeListener);
		removeKeyListener(treeListener);

		layout.setIs4AnnotationDialog(true);
	}

	public void setGlobalTreeBoolean(boolean b) {
		isGlobalPhylogeneticTreePanel = b;
	}

	public boolean isGlobalPhylogeneticTreePanel() {
		return isGlobalPhylogeneticTreePanel;
	}

	public ScaleBarProperty getScaleBarProperty() {
		return scaleBarProperty;
	}

	public GraphicsNode getRootNode() {
		return rootNode;
	}

	public void setIsShowAnnotation(boolean b) {
		layout.setIsShowAnnotation(b);

	}

	public AnnotationsProperties getMainAnnotationsProperties() {
		return mainAnnotationsProperties;
	}

	public void setMainAnnotationsProperties(AnnotationsProperties annotationsProperties) {
		this.mainAnnotationsProperties = annotationsProperties;
	}

	public AnnotationsProperties4LinageType getLinageTypeAnnotationsProperties() {
		return linageTypeAnnotationsProperties;
	}

	@Override
	public Container getHostComponent() {
		return this;
	}

	public void setLinageTypeAnnotationsProperties(AnnotationsProperties4LinageType linageTypeAnnotationsProperties) {
		this.linageTypeAnnotationsProperties = linageTypeAnnotationsProperties;
	}

	public void setWidthAndHeight(int width, int height) {
		currentWidth = width;
		currentHeight = height;
	}

	public Dimension getTreePanelDimension() {
		return new Dimension(currentWidth, currentHeight);
	}

	public void setLeafLabelsVisible(boolean visible) {
		var state = layoutProp.getShowLeafPropertiesInfo();
		boolean previous = state.isShowLeafLabel();
		state.setShowLeafLabel(visible);
		state.setNeedChange4showLabel(!previous && visible);
		state.setNeedChange4hideLabel(previous && !visible);
		module.evolview.phylotree.visualization.util.TreeOperationUtil.recursiveIterateTreeIF(rootNode, node -> {
			if (node.getChildCount() == 0) node.getDrawUnit().setDrawName(visible);
		});
		if (spiralFamily) spiralLeafLabels = visible; else otherLeafLabels = visible;
	}

	public void notifyTreeViewStateChanged() {
		firePropertyChange("treeViewState", null, this);
	}

	/** Structural traversal includes descendants hidden by collapse or label thinning. */
	public static List<GraphicsNode> subtreeNodes(GraphicsNode root) {
		List<GraphicsNode> nodes = new ArrayList<>();
		ArrayDeque<GraphicsNode> pending = new ArrayDeque<>();
		pending.push(root);
		while (!pending.isEmpty()) {
			GraphicsNode node = pending.pop();
			nodes.add(node);
			for (int i = node.getChildCount() - 1; i >= 0; i--) pending.push((GraphicsNode) node.getChildAt(i));
		}
		return nodes;
	}

	public boolean containsTreeNode(GraphicsNode node) {
		while (node != null) {
			if (node == rootNode) return true;
			node = (GraphicsNode) node.getParent();
		}
		return false;
	}

	public boolean canEditTreeNode(GraphicsNode node) {
		return containsTreeNode(node) && isTreeLayoutReady();
	}

	boolean isTreeLayoutReady() {
		return layout != null && layoutReady && !isLayoutCalculationPending();
	}

	public void selectSubtree(GraphicsNode node, boolean selected) {
		if (!containsTreeNode(node)) return;
		List<GraphicsNode> selection = layoutProp.getSelectedNodes();
		for (GraphicsNode descendant : subtreeNodes(node)) {
			if (selected) {
				if (!selection.contains(descendant)) selection.add(descendant);
			} else {
				selection.remove(descendant);
				descendant.getDrawUnit().setNodeSelected(false);
			}
			descendant.getDrawUnit().setBranchSelected(selected);
		}
		notifyTreeViewStateChanged();
		refreshGraphicSelectionControls();
		refreshViewPort();
	}

	/** Complete every structural edit before exposing it to layout and operation controls. */
	public void completeTreeStructureChange() {
		List<GraphicsNode> nodes = subtreeNodes(rootNode);
		Set<GraphicsNode> reachable = Collections.newSetFromMap(new IdentityHashMap<>());
		reachable.addAll(nodes);
		// Shared views may display just one clade; keep state belonging to other live clades.
		if (rootNode != layoutProp.getOriginalRootNode()) reachable.addAll(subtreeNodes(layoutProp.getOriginalRootNode()));
		layoutProp.getSelectedNodes().removeIf(node -> {
			if (reachable.contains(node)) return false;
			node.getDrawUnit().setBranchSelected(false);
			node.getDrawUnit().setNodeSelected(false);
			return true;
		});
		Set<Integer> ids = new java.util.HashSet<>();
		for (GraphicsNode node : reachable) ids.add(node.getID());
		layoutProp.getCollapsePropertyMaps().keySet().removeIf(id -> !ids.contains(id));
		mainAnnotationsProperties.retainTreeNodes(reachable);
		linageTypeAnnotationsProperties.retainTreeNodes(reachable);
		layoutProp.initializeRoot();
		recalculatePreservingZoom();
		notifyTreeViewStateChanged();
		refreshGraphicSelectionControls();
		refreshViewPort();
	}

	void refreshGraphicSelectionControls() {
		if (UnifiedAccessPoint.isGULaunched()) UnifiedAccessPoint.getInstanceFrame().refreshRightGraphicPropertiesPanel();
	}

	public boolean canRemoveSubtree(GraphicsNode target) {
		if (!canEditTreeNode(target) || target == rootNode) return false;
		Set<GraphicsNode> removed = Collections.newSetFromMap(new IdentityHashMap<>());
		removed.addAll(subtreeNodes(target));
		return subtreeNodes(rootNode).stream().anyMatch(node -> node.getChildCount() == 0 && !removed.contains(node));
	}

	/** Both removal actions confirm before changing topology, branch lengths or selection. */
	public boolean removeSubtree(GraphicsNode target, boolean contractUnaryParents) {
		if (!canRemoveSubtree(target)) return false;
		List<GraphicsNode> removed = subtreeNodes(target);
		long leaves = removed.stream().filter(node -> node.getChildCount() == 0).count();
		if (!confirmSubtreeRemoval(target, removed.size(), leaves, contractUnaryParents)) return false;
		if (!canRemoveSubtree(target)) return false;
		GraphicsNode parent = (GraphicsNode) target.getParent();
		parent.removeChild(target);
		while (parent != rootNode) {
			GraphicsNode grandParent = (GraphicsNode) parent.getParent();
			if (parent.getChildCount() == 0) {
				grandParent.removeChild(parent);
			} else if (contractUnaryParents && parent.getChildCount() == 1) {
				GraphicsNode child = (GraphicsNode) parent.getChildAt(0);
				child.setRealBranchLength(child.getRealBranchLength() + parent.getRealBranchLength());
				replaceChild(grandParent, parent, child);
			} else break;
			parent = grandParent;
		}
		completeTreeStructureChange();
		return true;
	}

	public static void replaceChild(GraphicsNode parent, GraphicsNode oldChild, GraphicsNode replacement) {
		List<GraphicsNode> children = new ArrayList<>();
		for (int i = 0; i < parent.getChildCount(); i++) {
			GraphicsNode child = (GraphicsNode) parent.getChildAt(i);
			children.add(child == oldChild ? replacement : child);
		}
		replacement.removeAllParent();
		parent.removeAllChild();
		for (GraphicsNode child : children) parent.addChild(child);
	}

	protected boolean confirmSubtreeRemoval(GraphicsNode target, int nodes, long leaves, boolean contractUnaryParents) {
		return createRemovalDialog(target, nodes, leaves, contractUnaryParents).showAndGetResult() == ModernDialog.DialogResult.OK;
	}

	protected ModernDialog createRemovalDialog(GraphicsNode target, int nodes, long leaves, boolean contractUnaryParents) {
		String name = target.getName();
		if (name == null || name.isBlank()) name = "Node " + target.getID();
		String message = "Delete subtree: " + name + "\nNodes: " + nodes + "\nLeaf nodes: " + leaves
				+ "\nIncludes descendants hidden by collapse."
				+ (contractUnaryParents ? "\nNon-root parents with one remaining child will be contracted; distances are preserved." : "");
		ModernDialog dialog = SwingDialog.builder().owner(SwingUtilities.getWindowAncestor(this))
				.type(ModernDialogType.QUESTION).title(contractUnaryParents ? "Delete clade" : "Detach node")
				.message(message).secondaryButton("Cancel", null).primaryButton("Delete", null).build();
		dialog.setSize(dialog.getWidth(), dialog.getHeight() + 60);
		setCancelDefault(dialog.getContentPane(), dialog.getRootPane());
		return dialog;
	}

	private static void setCancelDefault(Container container, JRootPane root) {
		for (Component component : container.getComponents()) {
			if (component instanceof JButton button && "Cancel".equals(button.getText())) {
				root.setDefaultButton(button);
				SwingUtilities.invokeLater(button::requestFocusInWindow);
			} else if (component instanceof Container child) setCancelDefault(child, root);
		}
	}

	public void clearNodeSelection() {
		layoutProp.getSelectedNodes().clear();
		evoltree.struct.util.EvolNodeUtil.recursiveIterateTreeIF(layoutProp.getOriginalRootNode(), node -> {
			node.getDrawUnit().setBranchSelected(false);
			node.getDrawUnit().setNodeSelected(false);
		});
		firePropertyChange("treeSelectionCleared", null, this);
		notifyTreeViewStateChanged();
		refreshGraphicSelectionControls();
		refreshViewPort();
	}

	public boolean supportsScaleBar() {
		return layout != null && layoutReady && !isLayoutCalculationPending() && layout.supportsScaleBar();
	}

	public boolean supportsAxisBar() { return layout != null && layoutReady && !isLayoutCalculationPending() && layout.supportsAxisBar(); }

	public String scaleUnavailableReason(boolean axis) {
		if (layout == null) return "Load a tree to use this control.";
		if (layoutProp.getMyLayout().isCladogram()) return layout.scaleUnavailableReason(axis);
		if (!layoutReady || isLayoutCalculationPending()) return "The tree layout is not ready. The scale will be available after a successful calculation.";
		return layout.scaleUnavailableReason(axis);
	}

	public boolean hasBootstrapValues() {
		final boolean[] present = {false};
		module.evolview.phylotree.visualization.util.TreeOperationUtil.recursiveIterateTreeIF(rootNode, node -> {
			if (node.getChildCount() > 0 && node.hasBootstrapValue()) present[0] = true;
		});
		return present[0];
	}

	public boolean isUniformScaleLayout() {
		String name = layoutProp.getMyLayout().name();
		return name.startsWith("RADIAL_") || name.startsWith("SPRIAL_") || name.contains("CIR_");
	}

	/** Percentages relative to the last fitted, readable canvas. */
	public Dimension getTreePanelScale() {
		if (fitCanvasSize.width <= 0 || fitCanvasSize.height <= 0) return new Dimension(100, 100);
		return new Dimension(Math.max(100, (int) Math.round(100.0 * currentWidth / fitCanvasSize.width)),
				Math.max(100, (int) Math.round(100.0 * currentHeight / fitCanvasSize.height)));
	}

	/** Manual scaling is centered on the visible viewport and requires no selected node. */
	public void setTreePanelScale(int heightPercent, int widthPercent) {
		if (layout == null || fitCanvasSize.width <= 0 || fitCanvasSize.height <= 0) return;
		heightPercent = Math.max(100, Math.min(100000, heightPercent));
		widthPercent = Math.max(100, Math.min(100000, widthPercent));
		if (isUniformScaleLayout()) heightPercent = widthPercent = Math.max(heightPercent, widthPercent);
		Rectangle visible = getVisibleRect();
		Point anchor = new Point((int) visible.getCenterX(), (int) visible.getCenterY());
		int targetHeight = (int) Math.round(fitCanvasSize.height * heightPercent / 100.0);
		int targetWidth = (int) Math.round(fitCanvasSize.width * widthPercent / 100.0);
		continuouslyZoomInOrOut(targetHeight - currentHeight, targetWidth - currentWidth, anchor);
		notifyTreeViewStateChanged();
	}

	public BaseLayout getCurrLayout() {
		return layout;
	}

	public TreeListener getTreeListener() {
		return treeListener;
	}
}
