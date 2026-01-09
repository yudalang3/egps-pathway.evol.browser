package module.evolview.pathwaybrowser;

import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;
import com.jidesoft.swing.JideTabbedPane.GradientColorProvider;
import egps2.EGPSProperties;
import egps2.UnifiedAccessPoint;
import egps2.builtin.modules.IconObtainer;
import egps2.frame.ModuleFace;
import egps2.modulei.IInformation;
import egps2.modulei.IModuleLoader;
import egps2.panels.dialog.SwingDialog;
import egps2.utils.common.util.SaveUtil;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.gfamily.work.listener.TreeListener;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.pathwaybrowser.gui.ControlPanelContainer;
import module.evolview.pathwaybrowser.gui.analysis.panel.*;
import module.evolview.pathwaybrowser.io.ImporterBean4PathwayFamilyBrowser;
import module.evolview.pathwaybrowser.io.Voice4pathwayFamilyBrowser;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.EGPSObjectsUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Pathway Family Browser Main Face
 *
 * This is the main UI controller for the pathway browser module.
 * It manages the entire interface including control panels, tree visualization, and analysis panels.
 */
@SuppressWarnings("serial")
public class PathwayFamilyMainFace extends ModuleFace {

	private static final Logger log = LoggerFactory.getLogger(PathwayFamilyMainFace.class);
	private static final String PERSISTENT_STORE_PATH = EGPSProperties.JSON_DIR.concat("/pathwayFamilyFace.json");

	// Core controller
	protected PathwayBrowserController controller;

	// Main panels
	protected ControlPanelContainer controlPanelContainer;
	protected JideTabbedPane tabbedPhylogeneticTreePane;  // Top-right: Tree visualization tabs
	protected JideTabbedPane tabbedAnalysisPanel;         // Bottom-right: Analysis result tabs

	// Layout components
	protected JSplitPane mainSplitPane;      // Horizontal split: Control panel | Right area
	protected JSplitPane rightSplitPanel;    // Vertical split: Tree panel | Analysis panel
	// UI properties
	protected Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();

	// Data handler
	private final Voice4pathwayFamilyBrowser voiceDataHandler = new Voice4pathwayFamilyBrowser(this);

	// Tab color provider for JIDE tabbed panes
	protected GradientColorProvider tabColorProvider = new GradientColorProvider() {
		Color TopBackGroundColor = new Color(196, 213, 231);

		@Override
		public Color getBackgroundAt(int tabIndex) {
			return Color.white;
		}

		@Override
		public Color getForegroundAt(int tabIndex) {
			return Color.black;
		}

		@Override
		public float getGradientRatio(int tabIndex) {
			return (float) 0.2;
		}

		@Override
		public Color getTopBackgroundAt(int tabIndex) {
			return TopBackGroundColor;
		}
	};

	protected PathwayFamilyMainFace(IModuleLoader moduleLoader) {
		super(moduleLoader);
	}

	@Override
	public boolean canImport() {
		return true;
	}

	@Override
	public void importData() {
		voiceDataHandler.doUserImportAction();
	}

	@Override
	public boolean canExport() {
		return true;
	}

	@Override
	public void exportData() {
		PhylogeneticTreePanel selectedPhylogeneticTreePanel = getSelectedPhylogeneticTreePanel();

		if (selectedPhylogeneticTreePanel == null) {
			SwingDialog.showErrorMSGDialog(UnifiedAccessPoint.getInstanceFrame(), "Export error",
					"There are no data in the remnant,\nPlease input data first.");
		} else {
			JideTabbedPane tabbedAnalysisPanel = getTabbedAnalysisPanel();
			int tabCount = tabbedAnalysisPanel.getTabCount();
			if (tabCount > 0) {
				// Export the selected analysis panel (PathwayDetailsPanel, etc.)
				Component selectedComponent = tabbedAnalysisPanel.getSelectedComponent();
				if (selectedComponent != null) {
					SaveUtil saveUtil = new SaveUtil();
//                    saveUtil.saveData(selectedComponent, getClass());
				}
			}
		}
	}

	@Override
	public String[] getFeatureNames() {
		return new String[]{"Pathway family visualization"};
	}

	@Override
	public void initializeGraphics() {
		initializeTheModule();
		importData();
	}

	/**
	 * Initialize the main UI module structure
	 *
	 * UI Layout Structure (界面布局结构):
	 * <pre>
	 * ┌─────────────────────────────────────────────────────────────────────────────────┐
	 * │ PathwayFamilyMainFace (Main Container)                                          │
	 * │ ┌─────────────────────────────────────────────────────────────────────────────┐ │
	 * │ │ mainSplitPane (JSplitPane, HORIZONTAL_SPLIT)                                │ │
	 * │ │ ┌──────────────────────┬───────────────────────────────────────────────────┐│ │
	 * │ │ │                      │                                                   ││ │
	 * │ │ │ LEFT AREA            │ RIGHT AREA (rightSplitPanel)                      ││ │
	 * │ │ │                      │ (JSplitPane, VERTICAL_SPLIT)                      ││ │
	 * │ │ │ Control Panel        │ ┌───────────────────────────────────────────────┐ ││ │
	 * │ │ │ Container            │ │ TOP: tabbedPhylogeneticTreePane               │ ││ │
	 * │ │ │                      │ │ (JideTabbedPane)                              │ ││ │
	 * │ │ │ ┌──────────────────┐ │ │ ┌─────────────────────────────────────────┐   │ ││ │
	 * │ │ │ │ JXTaskPane:      │ │ │ │ Tab: "Phylogeny"                        │   │ ││ │
	 * │ │ │ │ Tree Operation   │ │ │ │ ┌─────────────────────────────────────┐ │   │ ││ │
	 * │ │ │ │                  │ │ │ │ │ JScrollPane                         │ │   │ ││ │
	 * │ │ │ │ - Zoom controls  │ │ │ │ │  └─ PhylogeneticTreePanel           │ │   │ ││ │
	 * │ │ │ │ - Search         │ │ │ │ │     (Tree visualization)            │ │   │ ││ │
	 * │ │ │ │ - Select         │ │ │ │ │                                     │ │   │ ││ │
	 * │ │ │ │ - Color/Size     │ │ │ │ └─────────────────────────────────────┘ │   │ ││ │
	 * │ │ │ └──────────────────┘ │ │ └─────────────────────────────────────────┘   │ ││ │
	 * │ │ │                      │ └───────────────────────────────────────────────┘ ││ │
	 * │ │ │ ┌──────────────────┐ │                                                   ││ │
	 * │ │ │ │ JXTaskPane:      │ │ ════════════════════════════════════════════════ ││ │
	 * │ │ │ │ Tree Layout      │ │                                                   ││ │
	 * │ │ │ │                  │ │ ┌───────────────────────────────────────────────┐ ││ │
	 * │ │ │ │ - Rectangular    │ │ │ BOTTOM: tabbedAnalysisPanel                   │ ││ │
	 * │ │ │ │ - Circular       │ │ │ (JideTabbedPane)                              │ ││ │
	 * │ │ │ │ - Radial         │ │ │ ┌─────────────────────────────────────────┐   │ ││ │
	 * │ │ │ │ - Slope          │ │ │ │ Tab: "Pathway Details"                  │   │ ││ │
	 * │ │ │ │ - Spiral         │ │ │ │  └─ PathwayDetailsPanel                 │   │ ││ │
	 * │ │ │ └──────────────────┘ │ │ │     (Pathway component visualization)   │   │ ││ │
	 * │ │ │                      │ │ └─────────────────────────────────────────┘   │ ││ │
	 * │ │ │                      │ │ ┌─────────────────────────────────────────┐   │ ││ │
	 * │ │ │                      │ │ │ Tab: "Pathway Statistics"               │   │ ││ │
	 * │ │ │                      │ │ │  └─ PathwayStatisticsPanel              │   │ ││ │
	 * │ │ │                      │ │ │     (Statistical charts)                │   │ ││ │
	 * │ │ │                      │ │ └─────────────────────────────────────────┘   │ ││ │
	 * │ │ │                      │ │ ┌─────────────────────────────────────────┐   │ ││ │
	 * │ │ │                      │ │ │ Tab: "Evolutionary selection"           │   │ ││ │
	 * │ │ │                      │ │ │  └─ EvoSelectionPressurePanel           │   │ ││ │
	 * │ │ │                      │ │ │     (Selection pressure visualization)  │   │ ││ │
	 * │ │ │                      │ │ └─────────────────────────────────────────┘   │ ││ │
	 * │ │ │                      │ └───────────────────────────────────────────────┘ ││ │
	 * │ │ └──────────────────────┴───────────────────────────────────────────────────┘│ │
	 * │ └─────────────────────────────────────────────────────────────────────────────┘ │
	 * └─────────────────────────────────────────────────────────────────────────────────┘
	 *
	 * Component Hierarchy (组件层次结构):
	 *
	 * PathwayFamilyMainFace (this)
	 *  └─ mainSplitPane (JSplitPane, HORIZONTAL)
	 *      ├─ LEFT: controlPanelContainer (ControlPanelContainer)
	 *      │    └─ taskPaneContainer (JXTaskPaneContainer)
	 *      │        ├─ JXTaskPane: "Tree operation"
	 *      │        │   └─ CtrlTreeOperationPanelByMiglayout
	 *      │        │       ├─ Search/Select controls
	 *      │        │       ├─ Node color controls
	 *      │        │       ├─ Node size controls
	 *      │        │       └─ Branch thickness controls
	 *      │        │
	 *      │        └─ JXTaskPane: "Tree layout"
	 *      │            └─ CtrlTreeLayoutPanel (TreeLayoutSwitcher)
	 *      │                ├─ Layout type selector (Rectangular/Circular/Radial/Slope/Spiral)
	 *      │                └─ GUITreeZoomContainerInTopFixPanel (zoom controls)
	 *      │
	 *      └─ RIGHT: rightSplitPanel (JSplitPane, VERTICAL)
	 *           ├─ TOP: tabbedPhylogeneticTreePane (JideTabbedPane)
	 *           │    └─ Tab: "Phylogeny"
	 *           │        └─ JScrollPane
	 *           │            └─ PhylogeneticTreePanel
	 *           │                └─ Tree visualization (from gfamily shared component)
	 *           │
	 *           └─ BOTTOM: tabbedAnalysisPanel (JideTabbedPane)
	 *                ├─ Tab: "Pathway Details"
	 *                │    └─ PathwayDetailsPanel (SVG pathway diagram with gene mapping)
	 *                │
	 *                ├─ Tab: "Pathway Statistics"
	 *                │    └─ PathwayStatisticsPanel (bar charts of component counts)
	 *                │
	 *                └─ Tab: "Evolutionary selection"
	 *                     └─ EvoSelectionPressurePanel (selection pressure heatmap)
	 *
	 * Data Flow (数据流):
	 * 1. User clicks on tree node in PhylogeneticTreePanel
	 * 2. TreeListener fires event to tree2AnalyzingPanelInteractions
	 * 3. PathwayDetailsPanel and PathwayStatisticsPanel update to show selected species data
	 * 4. Analysis panels highlight relevant genes/components for the selected species
	 *
	 * </pre>
	 */
	protected void initializeTheModule() {
		// 1. Create controller
		controller = new PathwayBrowserController(this);

		// 2. Create main horizontal split pane
		mainSplitPane = getMainSplitPane();
		mainSplitPane.setBorder(null);

		// 3. Create left control panel container
		controlPanelContainer = new ControlPanelContainer(controller);
		mainSplitPane.setLeftComponent(controlPanelContainer);

		// 4. Create right vertical split pane
		getRightSplitPane();

		// 5. Load persistent UI state (divider positions)
		PathwayBrowserMainFaceBean bean = loadPersistentBean();
		double savedPerOfTreeAndBrowser = bean.getSavedPerOfTreeAndBrowser();
		int location = (int) (getHeight() * savedPerOfTreeAndBrowser);
		rightSplitPanel.setDividerLocation(location);
		mainSplitPane.setDividerLocation(bean.getSavedLocationOfMainSplitPanel());

		// 6. Create top-right tabbed pane for tree visualization
		tabbedPhylogeneticTreePane = new JideTabbedPane();
		tabbedPhylogeneticTreePane.setTabShape(JideTabbedPane.SHAPE_OFFICE2003);
		tabbedPhylogeneticTreePane.setTabPlacement(JideTabbedPane.TOP);

		tabbedPhylogeneticTreePane.setTabColorProvider(tabColorProvider);
		tabbedPhylogeneticTreePane.setTabEditingAllowed(true);
		tabbedPhylogeneticTreePane.setSelectedTabFont(controller.getTitleFont());
		tabbedPhylogeneticTreePane.setShowCloseButtonOnTab(true);
		tabbedPhylogeneticTreePane.setBoldActiveTab(true);

		rightSplitPanel.setLeftComponent(tabbedPhylogeneticTreePane);

		// 7. Create bottom-right tabbed pane for analysis panels
		getTabbedAnalysisPanel();
		rightSplitPanel.setRightComponent(tabbedAnalysisPanel);

		// 8. Assemble the layout
		mainSplitPane.setRightComponent(rightSplitPanel);
		add(mainSplitPane, BorderLayout.CENTER);
	}

	/**
	 * Load data and initialize the visualization panels
	 *
	 * @param geneData Gene family data including pathway figures and component information
	 * @param treeLayoutProperties Tree layout configuration and root node
	 */
	public void loadingDataInitializeGraphics(ImporterBean4PathwayFamilyBrowser geneData,
											  TreeLayoutProperties treeLayoutProperties) {

		// 1. Create and set up the phylogenetic tree panel
		GraphicsNode root = treeLayoutProperties.getOriginalRootNode();
		PhylogeneticTreePanel phylogeneticTreePanel = new PhylogeneticTreePanel(treeLayoutProperties, root,
				null, null);
		controller.setGlobalPhylogeneticTreePanel(phylogeneticTreePanel);
		controller.setTreeLayoutProperties(treeLayoutProperties);

		// 2. Add tree panel to the top-right tabbed pane
		ImageIcon imageIcon = IconObtainer.get("global.png");
		JScrollPane jScrollPane4tree = new JScrollPane(phylogeneticTreePanel);
		jScrollPane4tree.setBorder(BorderFactory.createEmptyBorder());

		List<PhylogeneticTreePanel> existedPhylogeneticTreePanels = getExistedPhylogeneticTreePanels();
		JideTabbedPane tabbedAnalysisPanel = getTabbedAnalysisPanel();

		// Clear existing tabs if any
		int tabCount = existedPhylogeneticTreePanels.size();
		if (tabCount > 0) {
			getTabbedPhylogeneticTreePane().removeAll();
			tabbedAnalysisPanel.removeAll();
		}
		getTabbedPhylogeneticTreePane().addTab("Phylogeny", imageIcon, jScrollPane4tree, "The referenced phylogenetic tree.");

		// 3. Setup tree-to-analysis-panel interactions
		TreeListener treeListener = phylogeneticTreePanel.getTreeListener();
		List<Consumer<GraphicsNode>> tree2AnalyzingPanelInteractions = treeListener
				.getTree2AnalyzingPanelInteractions();

		List<AbstractAnalysisPanel> analysisPanels = new ArrayList<>();

		// 4. Parse component information from TSV file
		String componentsInfoPath = geneData.getComponentsInfoPath();

		GuiCompCreator guiCompCreator = new GuiCompCreator(controller);

		Optional<PathwayGalleryPanel> pathwayGalleryPanel = guiCompCreator.createPathwayGalleryPanel(
				controller,
				geneData
		);

		if (pathwayGalleryPanel.isPresent()){
			PathwayGalleryPanel speciesPanel = pathwayGalleryPanel.get();
			// SpeciesInfoPanel already manages its own table scrolling.
			tabbedAnalysisPanel.addTab(speciesPanel.getTitle(), IconObtainer.get("tab.png"),
					speciesPanel, "The pathway details galleries.");

			tree2AnalyzingPanelInteractions.add(n -> {
				speciesPanel.treeNodeClicked(n.getName());
			});
			analysisPanels.add(speciesPanel);
		}

		Optional<SpeciesInfoPanel> speciesInforPanel = guiCompCreator.createSpeciesInfoPanel(
				geneData
		);

		if (speciesInforPanel.isPresent()){
			SpeciesInfoPanel speciesPanel = speciesInforPanel.get();
			// SpeciesInfoPanel already manages its own table scrolling.
			tabbedAnalysisPanel.addTab(speciesPanel.getTitle(), IconObtainer.get("tab.png"),
					speciesPanel, "The species information display.");

			tree2AnalyzingPanelInteractions.add(n -> {
				speciesPanel.treeNodeClicked(n.getName());
			});
			analysisPanels.add(speciesPanel);
		}
		Optional<PathwayComponentPanel> pathwayComponentPanel = guiCompCreator.createPathwayComponentPanel(
				geneData
		);

		if (pathwayComponentPanel.isPresent()){
			PathwayComponentPanel speciesPanel = pathwayComponentPanel.get();
			// SpeciesInfoPanel already manages its own table scrolling.
			tabbedAnalysisPanel.addTab(speciesPanel.getTitle(), IconObtainer.get("tab.png"),
					speciesPanel, "The pathway component count distribution on species.");

			tree2AnalyzingPanelInteractions.add(n -> {
				speciesPanel.treeNodeClicked(n.getName());
			});
			analysisPanels.add(speciesPanel);
		}

		Optional<SpeciesTraitPanel> speciesTraitPanel = guiCompCreator.createSpeciesTraitPanel(
				geneData
		);

		if (speciesTraitPanel.isPresent()){
			SpeciesTraitPanel traitPanel = speciesTraitPanel.get();
			// SpeciesTraitPanel already manages its own table scrolling.
			tabbedAnalysisPanel.addTab(traitPanel.getTitle(), IconObtainer.get("tab.png"),
					traitPanel, "The species trait information display.");

			tree2AnalyzingPanelInteractions.add(n -> {
				traitPanel.treeNodeClicked(n.getName());
			});
			analysisPanels.add(traitPanel);
		}



		SwingUtilities.invokeLater(() -> {
			for (AbstractAnalysisPanel analysisPanel : analysisPanels){
				analysisPanel.reInitializeGUI();
			}
		});
		SwingUtilities.invokeLater(() -> phylogeneticTreePanel.initializeLeftPanel());
	}

	@Override
	public IInformation getModuleInfo() {
		return new IInformation() {
			@Override
			public String getWhatDataInvoked() {
				return "The data is loading from the import dialog.";
			}

			@Override
			public String getSummaryOfResults() {
				return "The phylogenetic tree with pathway info. is generated by the eGPS software.";
			}
		};
	}

	@Override
	public boolean closeTab() {
		// Save control panel state
		if (controlPanelContainer != null) {
			controlPanelContainer.actionForSaveFile();
		}

		// Save split pane positions
		try {
			PathwayBrowserMainFaceBean bean = new PathwayBrowserMainFaceBean();
			if (mainSplitPane != null) {
				bean.setSavedLocationOfMainSplitPanel(mainSplitPane.getDividerLocation());
			}
			if (rightSplitPanel != null && getHeight() > 0) {
				double ratio = rightSplitPanel.getDividerLocation() / (double) getHeight();
				bean.setSavedPerOfTreeAndBrowser(ratio);
			}
			EGPSObjectsUtil.persistentSaveJavaBeanByFastaJSON(bean, new File(PERSISTENT_STORE_PATH));
		} catch (IOException e) {
			log.error("Error saving persistent state", e);
		}

		return false;
	}

	@Override
	public void changeToThisTab() {
		// Hook for tab change events
	}

	// ==================== Helper Methods ====================

	protected PathwayBrowserMainFaceBean loadPersistentBean() {
		PathwayBrowserMainFaceBean bean = null;
		File jsonFile = new File(PERSISTENT_STORE_PATH);
		if (jsonFile.exists()) {
			try {
				bean = EGPSObjectsUtil.obtainJavaBeanByFastaJSON(PathwayBrowserMainFaceBean.class, jsonFile);
			} catch (IOException e) {
				log.error("Error loading persistent state", e);
			}
		}
		if (bean == null) {
			bean = new PathwayBrowserMainFaceBean();
		}
		return bean;
	}

	public JideTabbedPane getTabbedAnalysisPanel() {
		if (tabbedAnalysisPanel == null) {
			tabbedAnalysisPanel = new JideTabbedPane();
			tabbedAnalysisPanel.setTabShape(JideTabbedPane.SHAPE_OFFICE2003);
			tabbedAnalysisPanel.setTabPlacement(JideTabbedPane.TOP);
			tabbedAnalysisPanel.setTabColorProvider(tabColorProvider);
			tabbedAnalysisPanel.setTabEditingAllowed(true);

			tabbedAnalysisPanel.setTabResizeMode(JideTabbedPane.RESIZE_MODE_FIT);

			Font defaultTitleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();
			tabbedAnalysisPanel.setFont(defaultTitleFont);
			tabbedAnalysisPanel.setBoldActiveTab(true);
		}
		return tabbedAnalysisPanel;
	}

	public JideTabbedPane getTabbedPhylogeneticTreePane() {
		return tabbedPhylogeneticTreePane;
	}

	protected JSplitPane getMainSplitPane() {
		if (mainSplitPane == null) {
			mainSplitPane = new JSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
			mainSplitPane.setDividerSize(10);
			mainSplitPane.setOneTouchExpandable(true);
		}
		return mainSplitPane;
	}

	protected JSplitPane getRightSplitPane() {
		if (rightSplitPanel == null) {
			rightSplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			rightSplitPanel.setDividerSize(10);
			rightSplitPanel.setOneTouchExpandable(true);
		}
		return rightSplitPanel;
	}

	public PhylogeneticTreePanel getSelectedPhylogeneticTreePanel() {
		if (tabbedPhylogeneticTreePane == null) {
			SwingDialog.showWarningMSGDialog("Not import data",
					"Sorry, you have not import data yet, Please load the phylogenetic tree.");
			return null;
		}

		Component selectedComponent = tabbedPhylogeneticTreePane.getSelectedComponent();
		if (selectedComponent == null) {
			return null;
		}

		PhylogeneticTreePanel phylogeneticTreePanel = null;
		if (selectedComponent instanceof PhylogeneticTreePanel) {
			phylogeneticTreePanel = (PhylogeneticTreePanel) selectedComponent;
		} else if (selectedComponent instanceof JScrollPane) {
			JScrollPane comp = (JScrollPane) selectedComponent;
			phylogeneticTreePanel = (PhylogeneticTreePanel) comp.getViewport().getView();
		}

		return phylogeneticTreePanel;
	}

	public List<PhylogeneticTreePanel> getExistedPhylogeneticTreePanels() {
		List<PhylogeneticTreePanel> ret = new ArrayList<>();
		if (tabbedPhylogeneticTreePane == null) {
			return ret;
		}

		Component[] components = tabbedPhylogeneticTreePane.getComponents();
		for (Component component : components) {
			PhylogeneticTreePanel phylogeneticTreePanel = null;
			if (component instanceof PhylogeneticTreePanel) {
				phylogeneticTreePanel = (PhylogeneticTreePanel) component;
			} else if (component instanceof JScrollPane) {
				JScrollPane comp = (JScrollPane) component;
				Object view = comp.getViewport().getView();
				if (view instanceof PhylogeneticTreePanel) {
					phylogeneticTreePanel = (PhylogeneticTreePanel) view;
				}
			}

			if (phylogeneticTreePanel != null) {
				ret.add(phylogeneticTreePanel);
			}
		}
		return ret;
	}

	public PhylogeneticTreePanel getGlobalPhylogeneticTreePanel() {
		if (controller != null) {
			return controller.getGlobalPhylogeneticTreePanel();
		}
		return null;
	}

	public PathwayBrowserController getController() {
		return controller;
	}
}
