package module.evolview.gfamily.work.gui.dialog;

import egps2.UnifiedAccessPoint;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.annotation.AnnotationsProperties;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * Copyright (c) 2019 Chinese Academy of Sciences. All rights reserved.
 *
 * @ClassName NodeAnnotationDialog.java
 *
 * @Package egps.remnant.phylogenetictree.util
 *
 * @author mhl
 * 
 * @version V1.0
 * 
 * @Date Created on: 2019-04-25 11:29
 * 
 */
public class NodeAnnotationDialogContainer extends JDialog {

	private static final long serialVersionUID = -8208821326033273500L;

	private JSplitPane splitPane;
	private JPanel jContentPane = null;
	private JPanel southJPanel = null;
	private JTree jTree;
	private JScrollPane rightView;

	private DefaultMutableTreeNode treeRoot;

//	private GeneFamilyController controller;
	private PhylogeneticTreePanel phylogeneticTreePanel;
	private PhylogeneticTreePanel mainPhylogeneticTreePanel;
	private GraphicsNode curreNode;

	AnnotationsProperties currentAnnotationsProperties = new AnnotationsProperties();

	private GraphicsNode virtureRoot;
	
	private Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();

	public NodeAnnotationDialogContainer(PhylogeneticTreePanel phylogeneticTreePanel2, GraphicsNode curreNode) {
		super(SwingUtilities.getWindowAncestor(phylogeneticTreePanel2), "Internal node Annotation", ModalityType.APPLICATION_MODAL);
		this.mainPhylogeneticTreePanel = phylogeneticTreePanel2;
		setSize(900, 450);
		setLocationRelativeTo(getOwner());
		this.curreNode = curreNode;
		setContentPane(getJContentPane());
		
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				actionsForExitWinodow();
			}
		});

	}

	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getCenterJPanel(), BorderLayout.CENTER);
			jContentPane.add(getSouthJPanel(), BorderLayout.SOUTH);

		}
		return jContentPane;
	}

	private JSplitPane getCenterJPanel() {
		if (splitPane == null) {
			splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			splitPane.setOneTouchExpandable(false);
			splitPane.setDividerSize(7);
			splitPane.setDividerLocation(250);

			treeRoot = new DefaultMutableTreeNode("");

			jTree = new JTree(treeRoot);
			jTree.setCellRenderer(new PreferenceTree());
			JScrollPane leftView = new JScrollPane(jTree);

			leftView.setBorder(BorderFactory.createLineBorder(Color.lightGray));

			DialogSidewardAnnotation peripheryAnnotation = new DialogSidewardAnnotation(this,
					"Sideward clade annotation", mainPhylogeneticTreePanel);
			treeRoot.add(peripheryAnnotation);

			DialogInternalNodeInsituAnnotation internalNodeAnnotation = new DialogInternalNodeInsituAnnotation(this,
					"Internal node in-situ annotation", mainPhylogeneticTreePanel);
			treeRoot.add(internalNodeAnnotation);

			DialogInternalNode2LeafAnnotation internalNode2leafAnnotation = new DialogInternalNode2LeafAnnotation(this,
					"Internal node to leaf annotation", mainPhylogeneticTreePanel);
			treeRoot.add(internalNode2leafAnnotation);

            DialogLeafNameAnnotation leafNameAnnotation = new DialogLeafNameAnnotation(this, "Leaf name annotation",
					mainPhylogeneticTreePanel);
            treeRoot.insert(leafNameAnnotation, 0);

            jTree.expandRow(0);
            jTree.setSelectionRow(1);
            jTree.setRootVisible(false);
            jTree.setFont(defaultFont);
            jTree.setRowHeight(20);
            //jTree.setBackground(new Color(240, 240, 240));

            rightView = new JScrollPane();
            rightView.setViewportView(leafNameAnnotation.getViewJPanel());
            rightView.setBorder(BorderFactory.createLineBorder(Color.lightGray));

            splitPane.add(leftView);

            splitPane.add(rightView);

            valueChangedJtree();

		}

		JSplitPane jSplitPane = new JSplitPane();
		jSplitPane.setOneTouchExpandable(false);
		jSplitPane.setDividerLocation(500);

		jSplitPane.setLeftComponent(splitPane);

		var previewProperties = new module.evolview.phylotree.visualization.layout.TreeLayoutProperties(getAVirtureRoot());
		previewProperties.setMyLayout(module.evolview.phylotree.visualization.graphics.struct.TreeLayout.RECT_CLADO_ALIGNED_LEFT);
		previewProperties.setGlobalFont(defaultFont.deriveFont(24f));
        previewProperties.setShowTitle(false);
		previewProperties.setShowAxisBar(false);
		previewProperties.setShowScaleBar(false);
		module.evolview.phylotree.visualization.layout.LeafLabelStateInitializer.applyInitialLeafLabelState(previewProperties, true);
		phylogeneticTreePanel = new PhylogeneticTreePanel(previewProperties,
				getAVirtureRoot(), null, null);
		phylogeneticTreePanel.setMainAnnotationsProperties(currentAnnotationsProperties);
		
		TitledBorder border = new TitledBorder(null, "Preview", TitledBorder.DEFAULT_JUSTIFICATION,
				TitledBorder.DEFAULT_POSITION, null, null);
		
		Font defaultTitleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();
		border.setTitleFont(defaultTitleFont);

		
		JPanel preview = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (phylogeneticTreePanel == null || phylogeneticTreePanel.getCurrLayout() == null) return;
                var layout = phylogeneticTreePanel.getCurrLayout();
                var originalFonts = new java.util.IdentityHashMap<module.evolview.phylotree.visualization.annotation.SidewardNodeAnnotation, Font>();
                for (var annotation : currentAnnotationsProperties.getOutterSidewardAnnos()) {
                    var style = annotation.getSidewardNodeAnnotation();
                    originalFonts.put(style, style.getTextFont());
                    style.setTextFont(style.getTextFont().deriveFont(Math.max(24f,style.getTextFont().getSize2D())));
                }
                var insituFonts = new java.util.IdentityHashMap<module.evolview.phylotree.visualization.annotation.DrawPropInternalNodeInsituAnno, Font>();
                for (var annotation : currentAnnotationsProperties.getInternalNode2LeafInsituAnnos()) {
                    insituFonts.put(annotation, annotation.getTextFont());
                    annotation.setTextFont(annotation.getTextFont().deriveFont(Math.max(24f,annotation.getTextFont().getSize2D())));
                }
                try {
                layout.calculateForPainting(400, 300);
                java.awt.geom.Rectangle2D bounds = new java.awt.geom.Rectangle2D.Double(0,0,400,300);
                var metrics = getFontMetrics(previewProperties.getGlobalFont());
                for (var leaf : previewProperties.getLeaves())
                    bounds = bounds.createUnion(new java.awt.geom.Rectangle2D.Double(leaf.getXSelf(),leaf.getYSelf()-metrics.getHeight(),metrics.stringWidth(leaf.getName())+12,metrics.getHeight()*2));
                for (var annotation : currentAnnotationsProperties.getOutterSidewardAnnos()) {
                    bounds = bounds.createUnion(annotation.getShape().getBounds2D());
                    var style = annotation.getSidewardNodeAnnotation();
                    var fm = getFontMetrics(style.getTextFont());
                    var point = annotation.getTextLocation();
                    double w = style.isDrawTextHorizontal()?fm.stringWidth(style.getTextString()):fm.getHeight();
                    double h = style.isDrawTextHorizontal()?fm.getHeight():fm.stringWidth(style.getTextString());
                    bounds = bounds.createUnion(new java.awt.geom.Rectangle2D.Double(point.x,point.y-h,w+12,h*2+12));
                }
                for (var annotation : currentAnnotationsProperties.getInternalNode2LeafInsituAnnos()) {
                    bounds = bounds.createUnion(annotation.getDrawShape().getBounds2D());
                    bounds = bounds.createUnion(annotation.getTextBounds(getFontMetrics(annotation.getTextFont())));
                }
                var insets = getInsets();
                double availableWidth = getWidth()-insets.left-insets.right-24;
                double availableHeight = getHeight()-insets.top-insets.bottom-24;
                if (availableWidth<=0 || availableHeight<=0) return;
                double scale = Math.min(1,Math.min(availableWidth/bounds.getWidth(),availableHeight/bounds.getHeight()));
                Graphics2D g = (Graphics2D)graphics.create();
                try {
                    g.translate(insets.left+12+(availableWidth-bounds.getWidth()*scale)/2, insets.top+12+(availableHeight-bounds.getHeight()*scale)/2);
                    g.scale(scale,scale); g.translate(-bounds.getX(),-bounds.getY()); layout.paintGraphics(g);
                } finally { g.dispose(); }
                } finally { originalFonts.forEach((style,font) -> style.setTextFont(font)); insituFonts.forEach((annotation,font) -> annotation.setTextFont(font)); }
            }
        };
        preview.setBackground(Color.WHITE); preview.setBorder(border);
        phylogeneticTreePanel.addPropertyChangeListener("treeViewState", e -> preview.repaint());
        jSplitPane.setRightComponent(preview);

		SwingUtilities.invokeLater(() -> {
			if (jTree == null || phylogeneticTreePanel == null) return;
			AbstractShowAnnotationDialog node = (AbstractShowAnnotationDialog) jTree.getLastSelectedPathComponent();
			node.configurateAnnotationProperty2preview();
			phylogeneticTreePanel.setWidthAndHeight(600, 400);
			phylogeneticTreePanel.updateViewAccording2TreeLayoutForAnnotationDialog();
		
		});

		return jSplitPane;
	}

	public GraphicsNode getAVirtureRoot() {
		if (virtureRoot == null) {

			virtureRoot = new GraphicsNode("Root");
			
			GraphicsNode leaf1 = new GraphicsNode("Leaf 1");
			GraphicsNode leaf2 = new GraphicsNode("Leaf 2");
			GraphicsNode leaf3 = new GraphicsNode("Leaf 3");
			GraphicsNode leaf4 = new GraphicsNode("Leaf 4");
			GraphicsNode iNode1 = new GraphicsNode("This is iNode1");
			GraphicsNode iNode2 = new GraphicsNode("This is iNode2");
			
			iNode1.addChild(leaf1);
			iNode1.addChild(leaf2);
			iNode2.addChild(leaf3);
			iNode2.addChild(leaf4);
			virtureRoot.addChild(iNode1);
			virtureRoot.addChild(iNode2);

		}

		return virtureRoot;
	}

	private void valueChangedJtree() {
		jTree.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				switchingOptions(jTree);
			}

			private void switchingOptions(JTree jTree) {
				AbstractShowAnnotationDialog node = (AbstractShowAnnotationDialog) jTree.getLastSelectedPathComponent();

				if (node == null) {
					return;
				}
				rightView.setViewportView(node.getViewJPanel());
				node.configurateAnnotationProperty2preview();
				phylogeneticTreePanel.updatePhylogeneticTreePanel();
			}
		});
	}

	private JPanel getSouthJPanel() {
		if (southJPanel == null) {
			southJPanel = new JPanel();
			southJPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			JButton applyAnnotation = new JButton("OK");
			JButton cancelAnnotation = new JButton("Cancel");

			applyAnnotation.addActionListener(e -> {
				AbstractShowAnnotationDialog node = (AbstractShowAnnotationDialog) jTree.getLastSelectedPathComponent();
				if (node == null) {
					return;
				}
				
				PhylogeneticTreePanel selectedPhylogeneticTreePanel = mainPhylogeneticTreePanel;
				AnnotationsProperties mainAnnotationsProperties = selectedPhylogeneticTreePanel.getMainAnnotationsProperties();
				mainAnnotationsProperties.addAnnotationProperties(currentAnnotationsProperties,curreNode);

				selectedPhylogeneticTreePanel.updatePhylogeneticTreePanel();
				dispose();
			});

			cancelAnnotation.addActionListener(e -> {
				actionsForExitWinodow();
			});

			applyAnnotation.setFont(defaultFont);
			cancelAnnotation.setFont(defaultFont);
			southJPanel.add(applyAnnotation);
			southJPanel.add(cancelAnnotation);
		}

		return southJPanel;
	}

	private void actionsForExitWinodow() {
		splitPane = null;
		jContentPane = null;
		southJPanel = null;
		jTree = null;
		rightView = null;
		treeRoot = null;
		defaultFont = null;
		phylogeneticTreePanel = null;
		curreNode = null;
		currentAnnotationsProperties = null;
		virtureRoot = null;

		mainPhylogeneticTreePanel.updatePhylogeneticTreePanel();
		dispose();
	}

	public GraphicsNode getCurreNode() {
		return curreNode;
	}

	public AnnotationsProperties getCurrentAnnotationsProperties() {
		return currentAnnotationsProperties;
	}
	
	public void updatePhylogeneticTreePanel() {
		phylogeneticTreePanel.updatePhylogeneticTreePanel();
	}

}
