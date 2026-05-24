package module.evolview.pathwaybrowser.gui.analysis.panel;

public class AnalysisPanelLoadVersionTest {

    public static void main(String[] args) {
        VersionedPanel panel = new VersionedPanel();

        int first = panel.startLoadForTest();
        int second = panel.startLoadForTest();

        if (panel.isCurrentForTest(first)) {
            throw new AssertionError("First load version must be stale after a newer load starts");
        }
        if (!panel.isCurrentForTest(second)) {
            throw new AssertionError("Latest load version must be current");
        }

        panel.applyIfCurrent(second, "new-result");
        panel.applyIfCurrent(first, "old-result");
        if (!"new-result".equals(panel.appliedValue)) {
            throw new AssertionError("Stale load result must not overwrite newer result");
        }
    }

    private static class VersionedPanel extends AbstractAnalysisPanel {
        private String appliedValue;

        private VersionedPanel() {
            super(null);
        }

        @Override
        public String getTitle() {
            return "versioned";
        }

        @Override
        public void reInitializeGUI() {
        }

        @Override
        public void treeNodeClicked(String nodeName) {
        }

        private int startLoadForTest() {
            return nextLoadVersion();
        }

        private boolean isCurrentForTest(int version) {
            return isLoadCurrent(version);
        }

        private void applyIfCurrent(int version, String value) {
            if (isLoadCurrent(version)) {
                appliedValue = value;
            }
        }
    }
}
