package module.evolview.pathwaybrowser.gui.tree.control;

import javax.swing.JPanel;

import module.evolview.pathwaybrowser.PathwayBrowserController;

@SuppressWarnings("serial")
public abstract class CommonSubLayoutPanel extends JPanel {
	protected PathwayBrowserController controller;


    public CommonSubLayoutPanel(PathwayBrowserController controller) {
        this.controller = controller;
    }


    public void setController(PathwayBrowserController controller) {
        this.controller = controller;
    }

}
