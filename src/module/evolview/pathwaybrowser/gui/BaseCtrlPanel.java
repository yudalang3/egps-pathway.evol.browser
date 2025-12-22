package module.evolview.pathwaybrowser.gui;

import java.awt.Font;

import javax.swing.JPanel;

import egps2.UnifiedAccessPoint;
import module.evolview.pathwaybrowser.PathwayBrowserController;

@SuppressWarnings("serial")
public abstract class BaseCtrlPanel extends JPanel {
	
	protected Font globalFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();


	
	protected PathwayBrowserController controller;
	/**
	 * 在这个过程中，你可以将所有的控制类的 setter都加进去
	 * @param controller
	 */
	public void setController(PathwayBrowserController controller) {
		this.controller = controller;
	}
	
	public void reInitializeGUIAccording2treeLayoutProperties() {

	}

}
