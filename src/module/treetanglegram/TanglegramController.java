package module.treetanglegram;

import java.awt.Dimension;
import java.awt.Font;
import java.io.File;

import javax.swing.JPanel;

import egps2.utils.common.util.SaveUtil;
import egps2.Authors;
import egps2.panels.dialog.SwingDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TanglegramController {

	private static final Logger log = LoggerFactory.getLogger(TanglegramController.class);

	private boolean savable = false;
	
	private final TanglegramMain main;

	public TanglegramController(TanglegramMain tanglegramMain) {
		main = tanglegramMain;
	}

	public String[] getTeamAndAuthors() {
		String[] info = new String[3];

		info[0] = "EvolGen";
		info[1] = Authors.YUDALANG + ","+ Authors.LIHAIPENG;
		info[2] = "http://www.picb.ac.cn/evolgen/";
		return info;
	}

	public boolean isSaveable() {
		return savable;
	}

	public void saveViewPanelAs() {
		new SaveUtil().saveData(getMain().getRightJPanel());
	}


	public void loadTab(File nwk1, File nwk2, Font nameFont , String outgroup) {
		
		Dimension paintingPanelDim = main.getPaintingPanelDim();
		JPanel loadTab;
		try {
			loadTab = new PaintingPanelPreparer().loadTab(nwk1, nwk2, nameFont , paintingPanelDim, outgroup);
		} catch (Exception e) {
			log.error("Failed to load tanglegram view tab.", e);
			SwingDialog.showErrorMSGDialog("Load error", "Failed to load tanglegram view.\n" + e.getMessage());
			return;
		}
		if (loadTab == null) {
			SwingDialog.showErrorMSGDialog("Load error", "Failed to load tanglegram view.");
			return;
		}
		
		main.setRightJPanel(loadTab);
		
		savable = true;
	}
	
	
	public TanglegramMain getMain() {
		return main;
	}


}
