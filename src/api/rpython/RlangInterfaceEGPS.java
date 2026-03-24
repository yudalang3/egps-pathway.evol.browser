package api.rpython;

import egps2.EGPSProperties;
import egps2.Launcher;
import egps2.Launcher4Dev;
import egps2.panels.dialog.SwingDialog;
import egps2.UnifiedAccessPoint;

import javax.swing.SwingUtilities;

/**
 * Java bridge for R-side integration.
 */
public class RlangInterfaceEGPS {

    /**
     * Launch the desktop application from an R-side call.
     */
    public String launchDesktop() throws Exception {
        Launcher.isLaunchFromR = true;
        if (!UnifiedAccessPoint.isGULaunched()) {
            Launcher4Dev.main(new String[]{});
        }
        return "Hello this is eGPS desktop, version: ".concat(EGPSProperties.EGPS_VERSION);
    }

    /**
     * Show a payload in a dialog and return its length for quick bridge testing.
     */
    public String showPayloadAndReturnLength(String jsonPayload) {
        SwingUtilities.invokeLater(() -> SwingDialog.showInfoMSGDialog("Info", jsonPayload));
        return String.valueOf(jsonPayload.length());
    }

    /**
     * Open Modern Tree View from a config file path.
     */
    public void openModernTreeView(String configFilePath) throws Exception {
        ModernTreeViewPyLauncher.launchFromConfigFile(configFilePath);
    }

    /**
     * Open Pathway Family Browser from a config file path.
     */
    public void openPathwayFamilyBrowser(String configFilePath) throws Exception {
        PathwayFamilyBrowserPyLauncher.launchFromConfigFile(configFilePath);
    }
}
