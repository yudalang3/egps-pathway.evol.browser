package api.rpython;

import egps2.Launcher;
import egps2.Launcher4Dev;
import egps2.UnifiedAccessPoint;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import egps2.frame.DefaultParamsAssignerAndParserHandler4VOICE;
import egps2.frame.MainFrameProperties;
import egps2.frame.ModuleFace;
import egps2.modulei.IModuleLoader;
import org.apache.commons.io.FileUtils;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.io.File;
import java.nio.charset.StandardCharsets;

final class ExternalGuiLauncherSupport {

    private static final int IMPORT_DELAY_MS = 1500;

    private ExternalGuiLauncherSupport() {
    }

    static void launchFromConfigFile(
            String configFilePath,
            ModuleLoaderFactory moduleLoaderFactory,
            ModuleImporter moduleImporter
    ) throws Exception {
        OrganizedParameterGetter organizedParameterGetter = parseConfigFile(configFilePath);

        Runnable launchAction = () -> {
            ModuleFace moduleFace = MainFrameProperties.loadTheModuleFromIModuleLoader(moduleLoaderFactory.create());
            Timer timer = new Timer(IMPORT_DELAY_MS, event -> {
                try {
                    moduleImporter.importInto(moduleFace, organizedParameterGetter);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            timer.setRepeats(false);
            timer.start();
        };

        if (UnifiedAccessPoint.isGULaunched()) {
            SwingUtilities.invokeLater(launchAction);
            return;
        }

        Launcher.isLaunchFromR = true;
        UnifiedAccessPoint.registerActionAfterMainFrame(launchAction);
        Launcher4Dev.main(new String[]{});
    }

    private static OrganizedParameterGetter parseConfigFile(String configFilePath) throws Exception {
        DefaultParamsAssignerAndParserHandler4VOICE defaultVoiceInputParamHandler =
                new DefaultParamsAssignerAndParserHandler4VOICE();
        VoiceParameterParser parser = defaultVoiceInputParamHandler.getParameterParser();
        String inputs = FileUtils.readFileToString(new File(configFilePath), StandardCharsets.UTF_8);
        return parser.getOrganizedParameterGetter(inputs);
    }

    interface ModuleLoaderFactory {
        IModuleLoader create();
    }

    interface ModuleImporter {
        void importInto(ModuleFace moduleFace, OrganizedParameterGetter organizedParameterGetter) throws Exception;
    }
}
