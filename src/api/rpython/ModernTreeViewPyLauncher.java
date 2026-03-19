package api.rpython;

import egps2.Launcher;
import egps2.UnifiedAccessPoint;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import egps2.frame.DefaultParamsAssignerAndParserHandler4VOICE;
import egps2.frame.MainFrameProperties;
import module.evolview.moderntreeviewer.IndependentModuleLoader;
import module.evolview.moderntreeviewer.MTreeViewMainFace;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Python-side launcher for opening Modern Tree View from a VOICE config file.
 */
public class ModernTreeViewPyLauncher {

    public static void launchFromConfigFile(String configFilePath) throws Exception {
        Launcher.main(new String[]{});

        DefaultParamsAssignerAndParserHandler4VOICE defaultVoiceInputParamHandler = new DefaultParamsAssignerAndParserHandler4VOICE();
        VoiceParameterParser parser = defaultVoiceInputParamHandler.getParameterParser();
        String inputs = FileUtils.readFileToString(new File(configFilePath), StandardCharsets.UTF_8);
        OrganizedParameterGetter organizedParameterGetter = parser.getOrganizedParameterGetter(inputs);

        UnifiedAccessPoint.registerActionAfterMainFrame(() -> {
            IndependentModuleLoader loader = new IndependentModuleLoader();
            loader.setWaitingText2loading();
            MTreeViewMainFace moduleFace = (MTreeViewMainFace) MainFrameProperties.loadTheModuleFromIModuleLoader(loader);
            module.evolview.moderntreeviewer.VOICE4MTV importHandler = moduleFace.getImportHandler();
            try {
                Thread.sleep(1500);
                importHandler.execute(organizedParameterGetter);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("A config file path is required.");
        }
        launchFromConfigFile(args[0]);
    }
}
