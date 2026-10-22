package module.evolview.moderntreeviewer;

import javax.swing.*;
import java.nio.file.*;
import module.evolview.moderntreeviewer.io.*;
import module.evolview.moderntreeviewer.para.TreeSettingsPreferences;

/** Standalone desktop acceptance harness using the actual MTV UI and export handler. */
public class NewickIntegrityGui {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of("temp/mtv-gui-acceptance"); Files.createDirectories(directory);
        var bean = new MTVImportInforBean(); bean.setNwkFormat(1);
        bean.setInput_nwk_string("('A,B':1234.56789,'O''Brien':0.000000123456789):3;");
        var root = new TreeParser4MTV().parseTree(bean).orElseThrow();
        SwingUtilities.invokeAndWait(() -> {
            var loader = new IndependentModuleLoader(); loader.setModuleData(root,null);
            var face = (MTreeViewMainFace)loader.getFace();
            face.treeSettingsPreferences = new TreeSettingsPreferences(directory.resolve("defaults.jsonc"));
            JFrame frame = new JFrame("MTV integrity acceptance"); frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JPanel actions = new JPanel();
            JButton export = new JButton("Export"); export.addActionListener(e -> face.exportData()); actions.add(export);
            JButton load = new JButton("Import"); load.addActionListener(e -> face.importData()); actions.add(load);
            frame.add(actions,java.awt.BorderLayout.NORTH); frame.add(face); frame.setSize(1200,800);
            frame.setLocationRelativeTo(null); frame.setVisible(true); face.initializeGraphics();
        });
    }
}
