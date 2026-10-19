package module.evolview.gfamily.work.gui.browser;

import egps2.UnifiedAccessPoint;
import module.evolview.gfamily.GeneFamilyController;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public abstract class BaseCtrlPanel extends JPanel {
	
	protected Font globalFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();

	
	protected GeneFamilyController controller;

	/** Semantic tags allow a module to attach its own parameter help without coupling shared controls. */
	protected static void parameter(JComponent component, String path) {
		component.putClientProperty("tree.parameter", path);
		component.setName(path);
	}
	/**
	 * 在这个过程中，你可以将所有的控制类的 setter都加进去
	 * @param controller
	 */
	public void setController(GeneFamilyController controller) {
		this.controller = controller;
	}
	
	public void reInitializeGUIAccording2treeLayoutProperties() {

	}

}
