package module.evolview.moderntreeviewer;

import com.google.common.base.Strings;
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

    private int exampleIndex = 0;
    private final java.util.concurrent.atomic.AtomicBoolean preparingExamples = new java.util.concurrent.atomic.AtomicBoolean();

    public VOICE4MTV(MTreeViewMainFace mTreeViewMainFace) {
        this.mTreeViewMainFace = mTreeViewMainFace;
    }

    @Override
    protected void actionBeforeVoiceGuiAppear() {
        exampleIndex = 0;
    }

    @Override
    protected int getNumberOfExamples() {
        return ModernTreeExamples.ALL.size();
    }

    @Override
    public void doUserImportAction() {
        if (!preparingExamples.compareAndSet(false, true)) return;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try { ModernTreeExamples.install(ModernTreeExamples.directory()); }
            catch (java.io.IOException error) { throw new java.util.concurrent.CompletionException(error); }
        }).whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
            preparingExamples.set(false);
            if (error != null) {
                Throwable cause = error.getCause() == null ? error : error.getCause();
                log.error("Unable to prepare tree examples", cause);
                SwingDialog.showErrorMSGDialog("Tree examples", cause.getMessage()
                        + "\nYou can still import your own tree file.");
            }
            super.doUserImportAction();
        }));
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

        String text = ModernTreeExamples.text(exampleIndex, ModernTreeExamples.directory());
        exampleIndex = (exampleIndex + 1) % getNumberOfExamples();
        return text;
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
            evolTreeParserMap4VOICM = new ParamsAssignerAndParser4ModernTreeView();
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
