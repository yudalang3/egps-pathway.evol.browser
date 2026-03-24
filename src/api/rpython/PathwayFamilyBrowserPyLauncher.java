package api.rpython;

import module.evolview.pathwaybrowser.IndependentModuleLoader;
import module.evolview.pathwaybrowser.PathwayFamilyMainFace;

/**
 * Python-side launcher for opening Pathway Family Browser from a VOICE config file.
 */
public class PathwayFamilyBrowserPyLauncher {

    public static void launchFromConfigFile(String configFilePath) throws Exception {
        ExternalGuiLauncherSupport.launchFromConfigFile(
                configFilePath,
                IndependentModuleLoader::new,
                (moduleFace, organizedParameterGetter) -> {
                    PathwayFamilyMainFace pathwayFamilyMainFace = (PathwayFamilyMainFace) moduleFace;
                    pathwayFamilyMainFace.getImportHandler().execute(organizedParameterGetter);
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
