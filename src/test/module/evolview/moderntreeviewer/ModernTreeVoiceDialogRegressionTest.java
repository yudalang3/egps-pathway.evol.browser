package module.evolview.moderntreeviewer;

import egps2.EGPSProperties;
import egps2.builtin.modules.voice.BookMarkNode;
import egps2.builtin.modules.voice.VersatileOpenInputClickAbstractGuiBase;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

/** Exercises real Modern Tree VOICE panels; run in a JVM with an isolated user.home. */
public final class ModernTreeVoiceDialogRegressionTest {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        Files.createDirectories(Path.of(EGPSProperties.JSON_DIR, "voice"));
        ModernTreeExamples.install(ModernTreeExamples.directory());
        var expected = new ArrayList<String>();
        for (int i = 0; i < ModernTreeExamples.ALL.size(); i++) {
            expected.add(ModernTreeExamples.text(i, ModernTreeExamples.directory()));
        }
        boolean showDialog = args.length == 1 && args[0].equals("--show-dialog");
        VOICE4MTV voice = new VOICE4MTV(null);
        for (int opening = 1; opening <= 4; opening++) {
            JPanel[] panel = new JPanel[1];
            JDialog[] dialog = new JDialog[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = voice.generateImportDataDialogGUI();
                if (showDialog) {
                    dialog[0] = new JDialog((Frame) null, "Input VOICE regression", false);
                    dialog[0].setContentPane(panel[0]);
                    dialog[0].setSize(1000, 900);
                    dialog[0].setLocationRelativeTo(null);
                    dialog[0].setVisible(true);
                }
            });
            int currentOpening = opening;
            try {
                // This task runs after the framework's deferred bookmark initialization.
                SwingUtilities.invokeAndWait(() -> {
                    JTree tree = find(panel[0], JTree.class);
                    JTextArea input = find(panel[0], JTextArea.class);
                    var root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                    var nodes = root.preorderEnumeration();
                    int index = 0;
                    while (nodes.hasMoreElements()) {
                        var node = (DefaultMutableTreeNode) nodes.nextElement();
                        if (!(node.getUserObject() instanceof BookMarkNode bookmark) || !bookmark.isExample()) continue;
                        if (!bookmark.getName().equals("Example-" + (index + 1))
                                || !bookmark.getContent().orElseThrow().equals(expected.get(index))) {
                            throw new AssertionError("Opening " + currentOpening + ": " + bookmark.getName()
                                    + " has the wrong example: " + bookmark.getContent().orElseThrow().lines().findFirst().orElseThrow());
                        }
                        if (index == 0) {
                            tree.setSelectionPath(new TreePath(node.getPath()));
                            voice.getEventUniformlyProcessor().action4singleLeftClick2anotherBookmark(bookmark);
                            if (!input.getText().equals(expected.getFirst())) {
                                throw new AssertionError("Selecting Example-1 must load the great-apes example");
                            }
                        }
                        index++;
                    }
                    if (index != 12) throw new AssertionError("Expected 12 built-in examples");
                });
            } finally {
                if (dialog[0] != null) SwingUtilities.invokeAndWait(dialog[0]::dispose);
            }
        }
        if (!expected.getFirst().contains("01-great-apes.nwk")) {
            throw new AssertionError("Example-1 must reference 01-great-apes.nwk");
        }
        LoggerFactory.getLogger(ModernTreeVoiceDialogRegressionTest.class).info(
                "All 12 Modern Tree examples retain their mapping across four VOICE openings; Example-1 uses 01-great-apes.nwk");
        LoggerFactory.getLogger(ModernTreeVoiceDialogRegressionTest.class).info("Framework loaded from {}",
                VersatileOpenInputClickAbstractGuiBase.class.getProtectionDomain().getCodeSource().getLocation());
    }

    private static <T extends Component> T find(Container container, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) return type.cast(component);
            if (component instanceof Container child) {
                T found = find(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
