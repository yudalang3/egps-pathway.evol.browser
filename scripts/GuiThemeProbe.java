import com.alibaba.fastjson.JSONObject;
import com.jidesoft.plaf.LookAndFeelFactory;
import egps2.Launcher;
import org.jdesktop.swingx.JXTaskPane;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Toolkit;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

/** Read-only GUI validation preflight; run with Java source-file mode. */
class GuiThemeProbe {
    public static void main(String[] args) {
        JSONObject result = new JSONObject(true);
        result.put("checked_at", ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
        result.put("java_home", System.getProperty("java.home"));
        result.put("java_runtime", System.getProperty("java.runtime.version"));
        result.put("os", System.getProperty("os.name"));
        result.put("swing_noxp", System.getProperty("swing.noxp"));
        try {
            Locale.setDefault(Locale.ENGLISH);
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            Launcher.lafPrepare();
            SwingUtilities.invokeAndWait(() -> {
                result.put("theme_active", Toolkit.getDefaultToolkit()
                        .getDesktopProperty("win.xpstyle.themeActive"));
                result.put("theme_color", Toolkit.getDefaultToolkit()
                        .getDesktopProperty("win.xpstyle.colorName"));
                result.put("look_and_feel", UIManager.getLookAndFeel().getClass().getName());
                result.put("task_pane_ui", new JXTaskPane().getUI().getClass().getName());
                Color title = UIManager.getColor("TaskPane.titleForeground");
                result.put("task_pane_title_rgb", title == null ? null
                        : title.getRed() + "," + title.getGreen() + "," + title.getBlue());
                result.put("jide_installed", LookAndFeelFactory.isJideExtensionInstalled());
            });
            var method = Class.forName("com.sun.java.swing.plaf.windows.XPStyle")
                    .getDeclaredMethod("getXP");
            method.setAccessible(true);
            result.put("xp_style_available", method.invoke(null) != null);
            boolean normal = Boolean.TRUE.equals(result.getBoolean("theme_active"))
                    && Boolean.TRUE.equals(result.getBoolean("xp_style_available"))
                    && Boolean.TRUE.equals(result.getBoolean("jide_installed"))
                    && System.getProperty("swing.noxp") == null
                    && "org.jdesktop.swingx.plaf.windows.WindowsTaskPaneUI"
                            .equals(result.getString("task_pane_ui"));
            result.put("status", normal ? "PASS" : "ENVIRONMENT_ABNORMAL");
        } catch (Throwable error) {
            result.put("status", "ENVIRONMENT_ABNORMAL");
            result.put("error", error.toString());
        }
        System.out.println(result.toJSONString());
        System.exit("PASS".equals(result.getString("status")) ? 0 : 2);
    }
}
