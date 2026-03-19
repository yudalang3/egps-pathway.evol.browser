package api.rpython;

import egps2.EGPSProperties;
import egps2.Launcher;
import egps2.Launcher4Dev;
import egps2.frame.MainFrameProperties;
import egps2.panels.dialog.SwingDialog;
import module.evolview.moderntreeviewer.IndependentModuleLoader;

import javax.swing.SwingUtilities;

/**
 * Java bridge for R-side integration.
 */
public class RlangInterfaceEGPS {

    /**
     * Launch the desktop application from an R-side call.
     */
    public String launchDesktop() throws Exception {
        Launcher4Dev.main(new String[]{});
        Launcher.isLaunchFromR = true;
        return "Hello this is eGPS desktop, version: ".concat(EGPSProperties.EGPS_VERSION);
    }

    /**
     * Backward-compatible legacy method.
     */
    public String launch() throws Exception {
        return launchDesktop();
    }

    /**
     * Show a payload in a dialog and return its length for quick bridge testing.
     */
    public String showPayloadAndReturnLength(String jsonPayload) {
        SwingUtilities.invokeLater(() -> SwingDialog.showInfoMSGDialog("Info", jsonPayload));
        return String.valueOf(jsonPayload.length());
    }

    /**
     * Backward-compatible legacy method.
     */
    public String callTest(String jsonStr) {
        return showPayloadAndReturnLength(jsonStr);
    }

    /**
     * Open the Modern Tree View module shell from an R-side call.
     */
    public void openModernTreeView(String jsonPayload) {
        SwingUtilities.invokeLater(() -> {
            IndependentModuleLoader loader = new IndependentModuleLoader();
            MainFrameProperties.loadTheModuleFromIModuleLoader(loader);
        });
    }

    /**
     * Backward-compatible legacy method.
     */
    public void modernTreeView(String jsonStr) {
        openModernTreeView(jsonStr);
    }
}
