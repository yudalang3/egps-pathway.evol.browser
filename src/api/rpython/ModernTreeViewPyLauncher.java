package api.rpython;

import module.evolview.moderntreeviewer.IndependentModuleLoader;
import module.evolview.moderntreeviewer.MTreeViewMainFace;

/**
 * Python-side launcher for opening Modern Tree View from a VOICE config file.
 */
public class ModernTreeViewPyLauncher {

    public static void launchFromConfigFile(String configFilePath) throws Exception {
        ExternalGuiLauncherSupport.launchFromConfigFile(
                configFilePath,
                () -> {
            IndependentModuleLoader loader = new IndependentModuleLoader();
            loader.setWaitingText2loading();
                    return loader;
                },
                (moduleFace, organizedParameterGetter) -> {
                    MTreeViewMainFace mtvMainFace = (MTreeViewMainFace) moduleFace;
                    module.evolview.moderntreeviewer.VOICE4MTV importHandler = mtvMainFace.getImportHandler();
                    importHandler.execute(organizedParameterGetter);
                }
        );
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("A config file path is required.");
        }
        launchFromConfigFile(args[0]);
    }
}
