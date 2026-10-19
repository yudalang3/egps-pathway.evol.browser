package module.evolview.moderntreeviewer;

import com.jidesoft.swing.JideSplitPane;
import egps2.EGPSProperties;
import egps2.UnifiedAccessPoint;
import egps2.frame.ModuleFace;
import egps2.frame.gui.EGPSMainGuiUtil;
import egps2.modulei.AdjusterFillAndLine;
import egps2.modulei.IInformation;
import egps2.modulei.IModuleLoader;
import egps2.panels.dialog.SwingDialog;
import egps2.utils.common.model.filefilter.SaveFilterNwk;
import egps2.utils.common.util.SaveUtil;
import module.evolview.gfamily.work.gui.CtrlTreeLayoutPanel;
import module.evolview.gfamily.work.gui.CtrlTreeOperationPanelByMiglayout;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.gui.CreativeModeTaskPanel;
import module.evolview.moderntreeviewer.para.TreeSettings;
import module.evolview.moderntreeviewer.para.TreeSettingsDialog;
import module.evolview.moderntreeviewer.para.TreeSettingsPreferences;
import module.evolview.phylotree.visualization.graphics.phylogeny.PhyloGraphicsTreeEncoderDecoder;
import module.evolview.phylotree.visualization.layout.LeafLabelStateInitializer;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.apache.commons.lang3.tuple.Pair;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import utils.storage.MapPersistence;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@SuppressWarnings("serial")
public class MTreeViewMainFace extends ModuleFace implements AdjusterFillAndLine, IInformation {

	JScrollPane scrollPane;
	private JXTaskPaneContainer taskPaneContainer;

	protected PhylogeneticTreePanel phylogeneticTreePanel;
	CreativeModeTaskPanel creativeModeTaskPanel;
	private JXTaskPane creativeModePane;
	private TreeLayoutProperties creativeModeProperties;
	TreeSettingsPreferences treeSettingsPreferences;
	private CtrlTreeLayoutPanel treeLayoutPanel;

	ModernTreeController controller = new ModernTreeController(null, this);
	private CtrlTreeOperationPanelByMiglayout treeOperationPanel;

	/**
	 * 为了持久化保存
	 */
	private Map<String, Integer> str2numberMap;
	private JSplitPane mainSplitPane;
	private final String dividerKey = "mtv.divider.location";

	private VOICE4MTV importHandler = new VOICE4MTV(this);

	private final String[] FEATURES = new String[] { "Convenient Operation", "Adjust graphics effects",
			"Display tree information", "Utilize differenct tree layout", "Toggle creative mode", };

	String howModuleLaunched = "Module launched by directly click the open button";
	String whatDataInvoked = "";

	MTreeViewMainFace(IModuleLoader moduleLoader) {
		super(moduleLoader);
		setLayout(new BorderLayout());

		add(getMainSplitPane(), BorderLayout.CENTER);
	}

	// ====================为了持久化保存
	private String getStorePath() {
		String storePath = EGPSProperties.JSON_DIR.concat("/egps.mtview.saveData.gz");
		return storePath;
	}

	private Map<String, Integer> getStr2numberMap() {
		if (str2numberMap == null) {
			String storePath = getStorePath();
			str2numberMap = MapPersistence.getStr2numberMap(storePath);
		}
		return str2numberMap;
	}

	private boolean getValueOfStr4storage(String key) {
		Map<String, Integer> str2numberMap2 = getStr2numberMap();
		Integer dividerLocation = str2numberMap2.get(key);
		if (dividerLocation == null) {
			dividerLocation = 0;
		}
		return dividerLocation.intValue() > 0;
	}
	// ====================为了持久化保存

	private JSplitPane getMainSplitPane() {
		mainSplitPane = new JSplitPane(JideSplitPane.HORIZONTAL_SPLIT);

		mainSplitPane.setDividerSize(7);

		Map<String, Integer> str2numberMap2 = getStr2numberMap();
		Integer dividerLocation = str2numberMap2.get(dividerKey);
		if (dividerLocation == null) {
			dividerLocation = 350;
		}
		mainSplitPane.setDividerLocation(dividerLocation);
		mainSplitPane.add(getLeftToolPane());

		scrollPane = new JScrollPane();
//		scrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
//			refresh();
//		});
//		scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
//			refresh();
//		});

		scrollPane.setBackground(Color.white);

		mainSplitPane.add(scrollPane);
		mainSplitPane.setOneTouchExpandable(true);
		mainSplitPane.setBorder(null);
		return mainSplitPane;
	}

	private JPanel getLeftToolPane() {
		JPanel jPanel4leftPanel = new JPanel(new BorderLayout());
		taskPaneContainer = new JXTaskPaneContainer();

		taskPaneContainer.setBackground(Color.WHITE);
		taskPaneContainer.setBackgroundPainter(null);

		addJXTaskPanels(taskPaneContainer);

		jPanel4leftPanel.setMinimumSize(new Dimension(290, 200));
		jPanel4leftPanel.add(new JScrollPane(taskPaneContainer), BorderLayout.CENTER);

		JButton jButton = new JButton("Advanced parameters");
		jButton.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogButtonFont());
		jButton.setToolTipText("Configure all tree parameters");
		JPanel advancedParametersHeader = new JPanel(new BorderLayout());
		advancedParametersHeader.setBorder(BorderFactory.createEmptyBorder(8, 8, 10, 8));
		advancedParametersHeader.add(jButton, BorderLayout.CENTER);
		jPanel4leftPanel.add(advancedParametersHeader, BorderLayout.NORTH);

		jButton.addActionListener(e -> {
			jButton.setEnabled(false);
			settingsPreferences().loadAsync().thenAccept(state -> SwingUtilities.invokeLater(() -> {
				jButton.setEnabled(true);
				showSettingsWarning(state);
				new TreeSettingsDialog(this, settingsPreferences(), state,
						phylogeneticTreePanel, () -> phylogeneticTreePanel).setVisible(true);
			}));
		});


		return jPanel4leftPanel;
	}

	private void addJXTaskPanels(JXTaskPaneContainer taskPaneContainer2) {
		Font titleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();
		{

			JXTaskPane taskPane = new JXTaskPane();
			taskPane.setFont(titleFont);
			String title = "Tree operation";
			taskPane.setTitle(title);
			treeOperationPanel = new CtrlTreeOperationPanelByMiglayout();
			boolean b = getValueOfStr4storage(title);
			taskPane.setCollapsed(b);

			taskPane.add(treeOperationPanel);
			taskPaneContainer2.add(taskPane);

		}
//		{
//			
//			JXTaskPane taskPane = new JXTaskPane();
//			taskPane.setFont(titleFont);
//			String title = "Tree operation";
//			taskPane.setTitle(title);
//			treeOperationPanel = new CtrlTreeOperationPanel();
//			boolean b = getValueOfStr4storage(title);
//			taskPane.setCollapsed(b);
//			
//			taskPane.add(new JScrollPane(treeOperationPanel));
//			taskPaneContainer2.add(taskPane);
//			
//		}

		{
			JXTaskPane taskPane = new JXTaskPane();
			taskPane.setFont(titleFont);
			String title = "Tree layout";
			taskPane.setTitle(title);
			treeLayoutPanel = new CtrlTreeLayoutPanel();
			JScrollPane comp = new JScrollPane(treeLayoutPanel);
			comp.setBorder(BorderFactory.createEmptyBorder());
			taskPane.add(comp);
			boolean b = getValueOfStr4storage(title);
			taskPane.setCollapsed(b);
			taskPaneContainer2.add(taskPane);
		}

		{
			JXTaskPane operationPane = new JXTaskPane() {
				@Override
				public void setCollapsed(boolean collapsed) {
					if (collapsed || isEnabled()) super.setCollapsed(collapsed);
				}
			};
			creativeModePane = operationPane;
			operationPane.setFont(titleFont);

			String title = "Creative mode";
			operationPane.setTitle(title);
			creativeModeTaskPanel = new CreativeModeTaskPanel();
			operationPane.add(creativeModeTaskPanel);
			operationPane.setEnabled(false);
			operationPane.setCollapsed(true);
			operationPane.setTitle("Creative mode (unavailable)");
			operationPane.setToolTipText(creativeModeTaskPanel.getToolTipText());

			taskPaneContainer2.add(operationPane);

		}
	}

	@Override
	public boolean closeTab() {
		Component[] components = taskPaneContainer.getComponents();

		Map<String, Integer> str2numberMap = getStr2numberMap();

		for (Component component : components) {
			if (component instanceof JXTaskPane) {
				JXTaskPane jxTaskPane = (JXTaskPane) component;
				Integer saveNumber = jxTaskPane.isCollapsed() ? 1 : 0;
				str2numberMap.put(jxTaskPane == creativeModePane ? "Creative mode" : jxTaskPane.getTitle(), saveNumber);
			}
		}

		str2numberMap.put(dividerKey, mainSplitPane.getDividerLocation());
		MapPersistence.storeStr2numberMap(str2numberMap, getStorePath());

		return false;
	}

	/**
	 * 让左边的控制面板刷新
	 */
	public void reInitializeGUIAccording2Properties() {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(this::reInitializeGUIAccording2Properties);
			return;
		}
		if (treeLayoutPanel != null) treeLayoutPanel.reInitializeGUIAccording2treeLayoutProperties();
		if (treeOperationPanel != null) treeOperationPanel.reInitializeGUIAccording2treeLayoutProperties();
		if (creativeModePane != null) {
			TreeLayoutProperties properties = controller.getActiveTreeLayoutProperties();
			boolean newTree = properties != creativeModeProperties;
			boolean wasAvailable = creativeModePane.isEnabled();
			creativeModeProperties = properties;
			creativeModeTaskPanel.setTreeLayoutProperties(properties);
			boolean available = creativeModeTaskPanel.isEnabled();
			creativeModePane.setEnabled(available);
			creativeModePane.setTitle(available ? "Creative mode" : "Creative mode (unavailable)");
			creativeModePane.setToolTipText(creativeModeTaskPanel.getToolTipText());
			if (!available) {
				creativeModePane.setCollapsed(true);
				if (phylogeneticTreePanel != null) phylogeneticTreePanel.setToolTipText(null);
			}
			else if (newTree || !wasAvailable) creativeModePane.setCollapsed(false);
		}
	}

	@Override
	public void changeToThisTab() {

	}

	@Override
	public boolean canImport() {
		return true;
	}

	@Override
	public void importData() {
		importHandler.doUserImportAction();

	}

	public VOICE4MTV getImportHandler() {
		return importHandler;
	}

	@Override
	public boolean canExport() {
		return true;
	}

	@Override
	public void exportData() {

		Consumer<String> txtAction = str -> {
			GraphicsNode rootNode = phylogeneticTreePanel.getRootNode();
			String code = new PhyloGraphicsTreeEncoderDecoder().encode(rootNode);
			try {
				String fileName = str.endsWith(".nwk") ? str :  str.concat(".nwk");
				Files.writeString(Path.of(fileName), code);
			} catch (IOException e) {
				e.printStackTrace();
			}
		};

		new SaveUtil().saveData(phylogeneticTreePanel, getClass(), Pair.of(new SaveFilterNwk(), txtAction));
	}

	@Override
	public void initializeGraphics() {
		settingsPreferences().loadAsync().thenAccept(state -> SwingUtilities.invokeLater(() -> {
			showSettingsWarning(state);
			initializeGraphicsWithSettings(state);
		}));
	}

	TreeSettingsPreferences settingsPreferences() {
		if (treeSettingsPreferences == null) treeSettingsPreferences = TreeSettingsPreferences.application();
		return treeSettingsPreferences;
	}

	void showSettingsWarning(TreeSettingsPreferences.State state) {
		if (settingsPreferences().claimWarning()) SwingDialog.showWarningMSGDialog("Advanced parameters", state.warning());
	}

	private void initializeGraphicsWithSettings(TreeSettingsPreferences.State settings) {
		// 这两个设置勿忘
		treeLayoutPanel.setController(controller);
		treeOperationPanel.setController(controller);

		creativeModeTaskPanel.setMain(this);

		Optional<IModuleLoader> moduleLoaderOpt = getModuleLoader();
		if (!moduleLoaderOpt.isPresent()) {
			throw new InputMismatchException("No module loader, please check by the developer.");
		}

		IndependentModuleLoader moduleLoader2 = (IndependentModuleLoader) moduleLoaderOpt.get();
		GraphicsNode rootNode = moduleLoader2.rootNode;
		if (rootNode == null) {
			String waitingTextMsg = moduleLoader2.waitingTextMsg;
			// 添加上用户引导性语句
			JPanel jPanel = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);
					Graphics2D g2d = (Graphics2D) g;
					EGPSMainGuiUtil.setupHighQualityRendering(g2d);
					EGPSMainGuiUtil.drawStringAtCenter(g2d,waitingTextMsg);
				}
			};
			this.scrollPane.setViewportView(jPanel);
			importData();
		} else {
			// 这个导入过程，如果更改需要注意三处：
			// VOICE4MTV类的execute()；
			// GeneFamilyMainFace类的initialize
			// 和MTreeViewMainFace的initializeGraphics()
			TreeLayoutProperties treeLayoutProperties = LeafLabelStateInitializer
					.resolveDirectLaunchProperties(rootNode, moduleLoader2.treeLayoutProperties);
			if (moduleLoader2.treeLayoutProperties == null) {
				TreeSettings.apply(treeLayoutProperties, TreeSettings.defaults());
				TreeSettings.apply(treeLayoutProperties, settings.values());
			}

			creativeModeTaskPanel.setTreeLayoutProperties(treeLayoutProperties);
			// 导入之后及时清除数据，避免对下次载入模块产生影响。
			moduleLoader2.rootNode = null;
			moduleLoader2.treeLayoutProperties = null;

			if (moduleLoader2.whatDataInvoked != null) {
				this.whatDataInvoked = moduleLoader2.whatDataInvoked;
			}
			if (moduleLoader2.howModuleLaunched != null) {
				this.howModuleLaunched = moduleLoader2.howModuleLaunched;
			}

			try {
				GraphicsNode convertNode = rootNode;
				this.phylogeneticTreePanel = new PhylogeneticTreePanel(treeLayoutProperties, convertNode, null, null);
				this.creativeModeTaskPanel.setTreeLayoutProperties(treeLayoutProperties);
				this.controller.setTreeLayoutProperties(treeLayoutProperties);
				// Remove the original one before add
				JViewport viewport = MTreeViewMainFace.this.scrollPane.getViewport();
				viewport.removeAll();
				viewport.add(MTreeViewMainFace.this.phylogeneticTreePanel);
			} catch (Exception e2) {
				SwingDialog.showErrorMSGDialog("Running error", e2.getMessage());
				e2.printStackTrace();

			}

			this.phylogeneticTreePanel.initializeLeftPanel();
            TreeSettings.initializeView(this.phylogeneticTreePanel);
			controller.syncTreeControlsFromModel();
		}

	}

	@Override
	public String[] getFeatureNames() {
		return FEATURES;
	}

	public void fitFrameRefresh() {
		if (phylogeneticTreePanel != null) {
			phylogeneticTreePanel.updatePhylogeneticTreePanel4fitFrame();
		}

	}

	public PhylogeneticTreePanel getSelectedPhylogenticTreePanel() {
		return phylogeneticTreePanel;
	}

	@Override
	public Optional<Color> couldSetFillColor() {
		return Optional.empty();
	}

	@Override
	public Optional<Font> couldSetFont() {
		return Optional.empty();
	}

	@Override
	public Optional<Color> couldSetLineColor() {
		TreeLayoutProperties treeLayoutProperties = controller.getTreeLayoutProperties();
		if (treeLayoutProperties == null) {
			return Optional.empty();
		}

		List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();

		if (selectedNodes.isEmpty()) {
			return Optional.empty();
		} else {
			GraphicsNode selectedNode = selectedNodes.get(0);
			Color lineColor = selectedNode.getDrawUnit().getLineColor();
			return Optional.of(lineColor);
		}

	}

	@Override
	public Optional<Integer> couldSetLineThickness() {
		TreeLayoutProperties treeLayoutProperties = controller.getTreeLayoutProperties();
		if (treeLayoutProperties == null) {
			return Optional.empty();
		}
		List<GraphicsNode> selectedNodes = treeLayoutProperties.getSelectedNodes();

		if (selectedNodes.isEmpty()) {
			return Optional.empty();
		} else {
			GraphicsNode selectedNode = selectedNodes.get(0);
			int size = (int) selectedNode.getDrawUnit().getStrokeSize();
			return Optional.of(size);
		}
	}

	@Override
	public void adjustFillColor(Color newCol) {

	}

	@Override
	public void adjustFillFont(Font newFont) {

	}

	@Override
	public void adjustLineColor(Color newCol) {
		List<GraphicsNode> selectedNodes = controller.getTreeLayoutProperties().getSelectedNodes();
		for (GraphicsNode graphicsNode : selectedNodes) {
			graphicsNode.getDrawUnit().setLineColor(newCol);
		}
		phylogeneticTreePanel.refreshViewPort();

		invokeTheFeatureMethod(1);
	}

	@Override
	public void adjustLineThickness(int newThickNess) {
		List<GraphicsNode> selectedNodes = controller.getTreeLayoutProperties().getSelectedNodes();
		for (GraphicsNode graphicsNode : selectedNodes) {
			graphicsNode.getDrawUnit().setStrokeSize(newThickNess);
		}

		phylogeneticTreePanel.refreshViewPort();
		invokeTheFeatureMethod(1);
	}

	@Override
	public String getHowModuleLaunch() {
		return howModuleLaunched;
	}

	@Override
	public String getHowUserOperates() {
		return "If you toggle the creative mode in control panel, the phylogenetic tree is manually created by user.<br>If you not utilize the creative mode, this is a pure visualization remnant, no operation on it.<br>";
	}

	@Override
	public String getWhatDataInvoked() {
		return whatDataInvoked;
	}

	@Override
	public String getSummaryOfResults() {
		return "The phylogenetic tree was visualize and operated in the eGPS software.";
	}

	@Override
	public IInformation getModuleInfo() {
		return this;
	}

}
