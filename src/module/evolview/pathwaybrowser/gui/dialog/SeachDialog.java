package module.evolview.pathwaybrowser.gui.dialog;

import egps2.UnifiedAccessPoint;
import module.evolview.pathwaybrowser.PathwayBrowserController;

import javax.swing.*;
import java.awt.*;

public class SeachDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	/**
	 * Create the dialog.
	 * 
	 * @param controller
	 */
	public SeachDialog(PathwayBrowserController controller) {
		super(UnifiedAccessPoint.getInstanceFrame(), true);
		setBounds(100, 100, 450, 200);
		setLayout(new BorderLayout());
		SearchDialogContent comp = new SearchDialogContent(controller);
		add(comp, BorderLayout.CENTER);

		setLocationRelativeTo(UnifiedAccessPoint.getInstanceFrame());
	}

}
