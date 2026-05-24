package test.module.evolview.pathwaybrowser.gui.analysis.panel;

import module.evolview.pathwaybrowser.gui.analysis.panel.AbstractAnalysisPanel;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbstractAnalysisPanelAsyncTest {

    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ranOnEdt = new AtomicBoolean(false);

        AbstractAnalysisPanel panel = new AbstractAnalysisPanel(null) {
            @Override
            public String getTitle() {
                return "test";
            }

            @Override
            public void reInitializeGUI() {
                ranOnEdt.set(SwingUtilities.isEventDispatchThread());
                latch.countDown();
            }

            @Override
            public void treeNodeClicked(String nodeName) {
            }
        };

        panel.reInitializeGUIAsync();

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("reInitializeGUIAsync did not invoke reInitializeGUI");
        }
        if (!ranOnEdt.get()) {
            throw new AssertionError("reInitializeGUIAsync must invoke default UI initialization on EDT");
        }
    }
}
