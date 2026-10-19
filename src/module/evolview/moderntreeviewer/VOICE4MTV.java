package module.evolview.moderntreeviewer;

import com.google.common.base.Strings;
import egps2.EGPSProperties;
import egps2.UnifiedAccessPoint;
import egps2.builtin.modules.voice.bean.AbstractParamsAssignerAndParser4VOICE;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.builtin.modules.voice.template.AbstractGuiBaseVoiceFeaturedPanel;
import egps2.panels.dialog.SwingDialog;
import module.evoltreio.EvolTreeImportInfoBean;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.io.MTVImportInforBean;
import module.evolview.moderntreeviewer.io.ParamsAssignerAndParser4ModernTreeView;
import module.evolview.moderntreeviewer.io.TreeParser4MTV;
import module.evolview.moderntreeviewer.io.TreePropertiesAssigner;
import module.evolview.moderntreeviewer.para.TreeSettings;
import module.evolview.moderntreeviewer.para.TreeSettingsPreferences;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;

/**
 * 这个类的移植还是有点麻烦，需要花时间让其更加通用
 */
public class VOICE4MTV extends AbstractGuiBaseVoiceFeaturedPanel {

    private static final Logger log = LoggerFactory.getLogger(VOICE4MTV.class);
    private final MTreeViewMainFace mTreeViewMainFace;

    private EvolTreeImportInfoBean current;

    private ParamsAssignerAndParser4ModernTreeView evolTreeParserMap4VOICM;

    private byte exampleIndex = 0;

    public VOICE4MTV(MTreeViewMainFace mTreeViewMainFace) {
        this.mTreeViewMainFace = mTreeViewMainFace;
    }

    @Override
    protected void actionBeforeVoiceGuiAppear() {
        exampleIndex = 0;
    }

    @Override
    protected int getNumberOfExamples() {
        return 3;
    }

    private boolean whetherSameImportInfoBean(EvolTreeImportInfoBean newBean) {
        if (current == null) {
            return false;
        }
        if (!Objects.equals(current.getInput_nwk_string(), newBean.getInput_nwk_string())) {
            return false;
        }

        if (!Objects.equals(current.getInput_nwk_path(), newBean.getInput_nwk_path())) {
            return false;
        }

        if (!Objects.equals(current.getInput_tableLike_path(), newBean.getInput_tableLike_path())) {
            return false;
        }

        return true;
    }

    @Override
    public String getExampleText() {

        evolTreeParserMap4VOICM = new ParamsAssignerAndParser4ModernTreeView();

        if (exampleIndex == 0) {
            // Great Apes phylogenetic tree with divergence times in MYA (Million Years Ago)
            // Human-Chimp: ~6.5 MYA, Chimp-Bonobo: ~1.5 MYA, Human/Chimp-Gorilla: ~8.5 MYA, Homininae-Orangutan: ~12.5 MYA
            String nwkStr = "(((Human:6.5,(Chimpanzee:1.5,Bonobo:1.5):5):2,Gorilla:8.5):4,Orangutan:12.5);";
            evolTreeParserMap4VOICM.addKeyValueEntryBean("input.nwk.string", nwkStr,
                    "Great Apes phylogeny with divergence times (MYA). Direct input the nwk string content.");
        } else if (exampleIndex == 1) {
            String speciesTree = EGPSProperties.PROPERTIES_DIR + "/bioData/example/treeBarplot/01.speci.nwk";
            evolTreeParserMap4VOICM.addKeyValueEntryBean("input.nwk.path", speciesTree, "Input the nwk file path.");
        } else if (exampleIndex == 2) {
            evolTreeParserMap4VOICM.addKeyValueEntryBean("input.nwk.path", "", "Input the nwk file path.");
            evolTreeParserMap4VOICM.addKeyValueEntryBean("input.tableLike.path",
                    EGPSProperties.PROPERTIES_DIR + "/bioData/example/myTree.tpv",
                    "Input the table-like tree file path.");
            exampleIndex = -1;
        }
        exampleIndex++;

        return evolTreeParserMap4VOICM.getExampleString().lines().map(line -> {
            String key = line.stripLeading();
            return !key.startsWith("#") && !key.startsWith("$input.") && key.contains("=") ? "# " + line : line;
        }).collect(java.util.stream.Collectors.joining("\n"));
    }

    @Override
    protected void setParameter(AbstractParamsAssignerAndParser4VOICE mapProducer) {
        // do not need to set the parameter, because the getExampleText is already
        // re-writed
    }

    @Override
    public void execute(OrganizedParameterGetter o) throws Exception {
        var loading = mTreeViewMainFace.settingsPreferences().loadAsync();
        if (!loading.isDone()) {
            loading.thenRunAsync(() -> {
                try { execute(o); }
                catch (Exception error) {
                    log.error("Unable to load the tree", error);
                    SwingUtilities.invokeLater(() -> SwingDialog.showErrorMSGDialog("Tree import", error.getMessage()));
                }
            });
            return;
        }
        TreeSettingsPreferences.State settings = loading.join();
        if (evolTreeParserMap4VOICM == null) {
            getExampleText();
        }

        // 这个导入过程，如果更改需要注意三处：VOICE4MTV类的execute()；GeneFamilyMainFace类的initialize和MTreeViewMainFace的initializeGraphics()

        try {
            MTVImportInforBean object = evolTreeParserMap4VOICM.generateTreeFromKeyValue(o);

            boolean whetherSameImportInfoBean = whetherSameImportInfoBean(object);
            if (whetherSameImportInfoBean) {
                return;
            }

            Optional<GraphicsNode> optRoot = new TreeParser4MTV().parseTree(object);
            if (!optRoot.isPresent()) {
                return;
            }


            GraphicsNode convertNode = optRoot.get();
            TreeLayoutProperties treeLayoutProperties = new TreeLayoutProperties(convertNode);

            TreePropertiesAssigner treePropertiesAssigner = new TreePropertiesAssigner();
            treePropertiesAssigner.assign(treeLayoutProperties, object);
            Map<String, Object> explicit = TreeSettings.explicitImport(o, treeLayoutProperties);
            TreeSettings.apply(treeLayoutProperties, TreeSettings.defaults());
            TreeSettings.apply(treeLayoutProperties, settings.values());
            TreeSettings.apply(treeLayoutProperties, explicit);
            treePropertiesAssigner.assignGraphicsNodeEffects(convertNode, object);

            String dataSource = null;
            if (!Strings.isNullOrEmpty(object.getInput_nwk_string())) {
                dataSource = "Data is loading from nwk string.";
            } else if (!Strings.isNullOrEmpty(object.getInput_nwk_path())) {
                File file = new File(object.getInput_nwk_path());
                dataSource = "Data is loading from file: ".concat(file.getName());
            } else {
                dataSource = "Data is loading from eGPS-defined table like string ";
            }

            String loadedDataSource = dataSource;
            SwingUtilities.invokeLater(() -> displayTree(convertNode, treeLayoutProperties, loadedDataSource, settings));
        } catch (Exception e2) {
            log.error("Unable to import the tree", e2);
            SwingUtilities.invokeLater(() -> SwingDialog.showErrorMSGDialog("Running error", e2.getMessage()));
            return;
        }
        log.trace("Finish loading tree.");
    }

    private void displayTree(GraphicsNode root, TreeLayoutProperties properties, String source,
                             TreeSettingsPreferences.State settings) {
        mTreeViewMainFace.showSettingsWarning(settings);
        mTreeViewMainFace.whatDataInvoked = source;
        mTreeViewMainFace.phylogeneticTreePanel = new PhylogeneticTreePanel(properties, root, null, null);
        mTreeViewMainFace.creativeModeTaskPanel.setTreeLayoutProperties(properties);
        mTreeViewMainFace.controller.setTreeLayoutProperties(properties);
        // 要把原来的viewport移除，再新增
        JViewport viewport = mTreeViewMainFace.scrollPane.getViewport();
        viewport.removeAll();
        log.trace("Count is: {}", viewport.getComponentCount());
        viewport.add(mTreeViewMainFace.phylogeneticTreePanel);
        log.trace("Count is: {}, parent is {}", viewport.getComponentCount(), mTreeViewMainFace.phylogeneticTreePanel.getParent());
        log.trace("phylogeneticTreePanel, parent is {}", mTreeViewMainFace.phylogeneticTreePanel.getParent());

        mTreeViewMainFace.phylogeneticTreePanel.initializeLeftPanel();
        TreeSettings.initializeView(mTreeViewMainFace.phylogeneticTreePanel);
        log.trace("Tree panel initialized.");
        mTreeViewMainFace.controller.syncTreeControlsFromModel();
        log.trace("mTreeViewMainFace.controller.syncTreeControlsFromModel;");
    }

}
